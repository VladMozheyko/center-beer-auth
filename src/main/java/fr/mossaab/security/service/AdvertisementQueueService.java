package fr.mossaab.security.service;

import fr.mossaab.security.dto.advertisement.AdTimeLeftResponse;
import fr.mossaab.security.entities.Advertisement;
import fr.mossaab.security.enums.AdQueueStatus;
import fr.mossaab.security.enums.AdvertisementStatus;
import fr.mossaab.security.repository.AdvertisementRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;


import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdvertisementQueueService {

    private final AdvertisementRepository advertisementRepository;
    private final TaskScheduler taskScheduler;

    private final ReentrantLock lock = new ReentrantLock();
    private ScheduledFuture<?> currentLeaderTask;

    @PostConstruct
    public void init() {
        updateLeadership(); // запуск при старте
    }

    public AdTimeLeftResponse getRemainingTimeForCurrentLeader() {
        Optional<Advertisement> currentOpt = getCurrentLeader();
        if (currentOpt.isEmpty()) {
            return new AdTimeLeftResponse(0, 0, "Нет активного лидера рекламы");
        }

        Advertisement current = currentOpt.get();
        LocalDateTime now = LocalDateTime.now();

        if (current.getStartTime() == null) {
            return new AdTimeLeftResponse(0, 0, "Реклама ещё не начала отображаться");
        }

        long secondsPassed = Duration.between(current.getStartTime(), now).toSeconds();

        // Проверка: есть ли перебивка
        List<Advertisement> queue = getQueue();
        Optional<Advertisement> stronger = queue.stream()
                .filter(ad -> ad.getCost() > current.getCost())
                .findFirst();

        if (stronger.isPresent()) {
            long remainingToHourSec = 60 * 60 - secondsPassed;
            long remainingSec = Math.min(15 * 60, remainingToHourSec);

            int min = (int) (remainingSec / 60);
            int sec = (int) (remainingSec % 60);
            String msg = String.format("Текущая реклама будет показываться ещё %02d:%02d (перебита, но висит минимум)", min, sec);
            return new AdTimeLeftResponse(min, sec, msg);
        }

        if (secondsPassed >= 3600) {
            return new AdTimeLeftResponse(0, 0, "Период показа завершён");
        }

        long secondsLeft = 3600 - secondsPassed;
        int min = (int) (secondsLeft / 60);
        int sec = (int) (secondsLeft % 60);
        String msg = String.format("Осталось %02d:%02d до завершения показа текущей рекламы", min, sec);

        return new AdTimeLeftResponse(min, sec, msg);
    }



    public Optional<Advertisement> getCurrentLeader() {
        updateLeadership();
        return advertisementRepository.findAll().stream()
                .filter(ad -> ad.getStatus() == AdvertisementStatus.APPROVED)
                .filter(ad -> ad.getQueueStatus() == AdQueueStatus.LEADING)
                .findFirst();
    }

    public List<Advertisement> getQueue() {
        return advertisementRepository.findAll().stream()
                .filter(ad -> ad.getStatus() == AdvertisementStatus.APPROVED)
                .filter(ad -> ad.getQueueStatus() == AdQueueStatus.WAITING)
                .filter(ad -> ad.getStartTime() == null)
                .sorted(Comparator.comparingInt(Advertisement::getCost).reversed()
                        .thenComparing(Advertisement::getCreatedAt))
                .collect(Collectors.toList());
    }

    public void updateLeadership() {
        lock.lock();
        try {
            Optional<Advertisement> current = getCurrentLeader();
            LocalDateTime now = LocalDateTime.now();

            if (current.isEmpty()) {
                promoteNextFromQueue();
                return;
            }

            Advertisement currentLeader = current.get();
            long minutes = Duration.between(currentLeader.getStartTime(), now).toMinutes();

            // Завершение через 60 минут
            if (minutes >= 60) {
                currentLeader.setQueueStatus(AdQueueStatus.COMPLETED);
                advertisementRepository.save(currentLeader);
                promoteNextFromQueue();
                return;
            }

            // Проверка на перебивку
            List<Advertisement> betterAds = getQueue().stream()
                    .filter(ad -> ad.getCost() > currentLeader.getCost())
                    .collect(Collectors.toList());

            if (!betterAds.isEmpty()) {
                long remaining = 60 - minutes;
                long delay = Math.min(15, remaining);
                scheduleLeadershipChange(betterAds.get(0), delay);
            }

        } finally {
            lock.unlock();
        }
    }

    private void scheduleLeadershipChange(Advertisement newLeader, long delayMinutes) {
        if (currentLeaderTask != null) currentLeaderTask.cancel(false);
        currentLeaderTask = taskScheduler.schedule(() -> {
            lock.lock();
            try {
                getCurrentLeader().ifPresent(current -> {
                    current.setQueueStatus(AdQueueStatus.COMPLETED);
                    advertisementRepository.save(current);
                });

                newLeader.setQueueStatus(AdQueueStatus.LEADING);
                newLeader.setStartTime(LocalDateTime.now());
                advertisementRepository.save(newLeader);
            } finally {
                lock.unlock();
            }
        }, Date.from(LocalDateTime.now().plusMinutes(delayMinutes).atZone(TimeZone.getDefault().toZoneId()).toInstant()));
    }

    private void promoteNextFromQueue() {
        List<Advertisement> queue = getQueue();
        if (queue.isEmpty()) return;

        Advertisement next = queue.get(0);
        next.setQueueStatus(AdQueueStatus.LEADING);
        next.setStartTime(LocalDateTime.now());
        advertisementRepository.save(next);

        if (currentLeaderTask != null) currentLeaderTask.cancel(false);
        currentLeaderTask = taskScheduler.schedule(() -> {
            lock.lock();
            try {
                next.setQueueStatus(AdQueueStatus.COMPLETED);
                advertisementRepository.save(next);
                promoteNextFromQueue();
            } finally {
                lock.unlock();
            }
        }, Date.from(LocalDateTime.now().plusHours(1).atZone(TimeZone.getDefault().toZoneId()).toInstant()));
    }

    public int calculateAdRevenueForLastHour() {
        return advertisementRepository.findAll().stream()
                .filter(ad -> ad.getQueueStatus() == AdQueueStatus.LEADING)
                .filter(ad -> ad.getStartTime() != null)
                .filter(ad -> ad.getStartTime().isAfter(LocalDateTime.now().minusHours(1)))
                .mapToInt(Advertisement::getCost)
                .sum();
    }

    public void resetAdQueue() {
        List<Advertisement> all = advertisementRepository.findAll();
        for (Advertisement ad : all) {
            ad.setQueueStatus(AdQueueStatus.WAITING);
            ad.setStartTime(null);
        }
        advertisementRepository.saveAll(all);
    }
}
