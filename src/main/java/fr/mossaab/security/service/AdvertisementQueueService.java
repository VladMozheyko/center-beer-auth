package fr.mossaab.security.service;

import fr.mossaab.security.dto.advertisement.AdTimeLeftResponse;
import fr.mossaab.security.entities.Advertisement;
import fr.mossaab.security.enums.AdQueueStatus;
import fr.mossaab.security.enums.AdvertisementStatus;
import fr.mossaab.security.repository.AdvertisementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdvertisementQueueService {

    private final AdvertisementRepository advertisementRepository;
    private final TaskScheduler taskScheduler;

    private final ReentrantLock lock = new ReentrantLock();

    // Задача по автоматической смене лидера через 1 час
    private ScheduledFuture<?> currentLeaderTask;
    // Задача по отложенной замене через 15 минут
    private ScheduledFuture<?> replacementTask;
    // Кандидат на замену, для которого уже запланирована replacementTask
    private Advertisement pendingReplacement;
    public int calculateAdRevenueForLastHour() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(1);
        return advertisementRepository.findAll().stream()
                .filter(ad -> ad.getQueueStatus() == AdQueueStatus.LEADING)
                .filter(ad -> ad.getStartTime() != null && ad.getStartTime().isAfter(cutoff))
                .mapToInt(Advertisement::getCost)
                .sum();
    }


    /**
     * Принудительная смена лидера рекламы (без учёта 15‑минутного ограничения).
     */
    public void forceSwitchToNextLeader() {
        lock.lock();
        try {
            cancelPendingTasks();
            completeCurrentLeader();
            promoteNextFromQueue();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Основной метод, запускаемый каждые 60 секунд:
     * 1) Завершает лидера, если прошло ≥1 час.
     * 2) Если есть более высокая ставка — мгновенно (после 15 мин) или отложенно (до 15 мин) планирует замену.
     */
    public void updateLeadership() {
        lock.lock();
        try {
            Optional<Advertisement> currentOpt = findCurrentLeader();
            LocalDateTime now = LocalDateTime.now();

            // Если лидера нет — сразу назначаем следующего
            if (currentOpt.isEmpty()) {
                promoteNextFromQueue();
                return;
            }

            Advertisement leader = currentOpt.get();
            long secondsSinceStart = Duration.between(leader.getStartTime(), now).getSeconds();

            // 1) Если висит ≥ 1 час — завершаем и назначаем следующего
            if (secondsSinceStart >= 3600) {
                cancelPendingTasks();
                leader.setQueueStatus(AdQueueStatus.COMPLETED);
                advertisementRepository.save(leader);
                promoteNextFromQueue();
                return;
            }

            // 2) Ищем более дорогие объявления в очереди
            List<Advertisement> betterAds = getQueue().stream()
                    .filter(ad -> ad.getCost() > leader.getCost())
                    .collect(Collectors.toList());

            if (betterAds.isEmpty()) {
                // нет перебивок
                return;
            }

            Advertisement topChallenger = betterAds.get(0);

            // 3) Если прошло ≥ 15 мин — мгновенная замена
            if (secondsSinceStart >= 15 * 60) {
                cancelPendingTasks();
                replaceLeader();
            } else {
                // 4) Иначе — планируем замену на 15‑ю минуту (если ещё не запланировано)
                long delaySec = 15 * 60 - secondsSinceStart;
                if (replacementTask == null || !topChallenger.equals(pendingReplacement)) {
                    cancelReplacementTask();
                    pendingReplacement = topChallenger;
                    replacementTask = taskScheduler.schedule(() -> {
                        lock.lock();
                        try {
                            replaceLeader();
                        } finally {
                            lock.unlock();
                        }
                    }, Date.from(Instant.now().plusSeconds(delaySec)));
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Возвращает текущего лидера (без изменения состояний).
     */
    public Optional<Advertisement> getCurrentLeader() {
        return findCurrentLeader();
    }

    /**
     * Завершает текущего лидера и назначает нового из очереди.
     */
    private void replaceLeader() {
        completeCurrentLeader();
        promoteNextFromQueue();
    }

    /**
     * Завершает (помечает как COMPLETED) текущее ведущие объявление.
     */
    private void completeCurrentLeader() {
        findCurrentLeader().ifPresent(curr -> {
            curr.setQueueStatus(AdQueueStatus.COMPLETED);
            advertisementRepository.save(curr);
        });
    }

    /**
     * Назначает в лидеры первое объявление из очереди и ставит таймер на 1 час.
     */
    private void promoteNextFromQueue() {
        List<Advertisement> queue = getQueue();
        if (queue.isEmpty()) return;

        Advertisement next = queue.get(0);
        next.setQueueStatus(AdQueueStatus.LEADING);
        next.setStartTime(LocalDateTime.now());
        advertisementRepository.save(next);

        // Сбрасываем все отложенные задачи и ставим новую на 1 час
        cancelPendingTasks();
        currentLeaderTask = taskScheduler.schedule(() -> {
            lock.lock();
            try {
                next.setQueueStatus(AdQueueStatus.COMPLETED);
                advertisementRepository.save(next);
                promoteNextFromQueue();
            } finally {
                lock.unlock();
            }
        }, Date.from(Instant.now().plusSeconds(3600)));
    }

    /**
     * Возвращает DTO с оставшимся временем и сообщением для текущего лидера.
     */
    public AdTimeLeftResponse getRemainingTimeForCurrentLeader() {
        Optional<Advertisement> currentOpt = findCurrentLeader();
        if (currentOpt.isEmpty()) {
            return new AdTimeLeftResponse(0, 0, "Нет активного лидера рекламы");
        }
        Advertisement current = currentOpt.get();
        long secondsSinceStart = Duration.between(current.getStartTime(), LocalDateTime.now()).toSeconds();
        long secondsLeft = Math.max(0, 3600 - secondsSinceStart);

        boolean hasStronger = getQueue().stream()
                .anyMatch(ad -> ad.getCost() > current.getCost());

        String msg;
        if (hasStronger) {
            if (secondsSinceStart < 15 * 60) {
                long until15 = 15 * 60 - secondsSinceStart;
                msg = String.format(
                        "Текущая реклама перебита, но будет показываться ещё минимум %02d:%02d",
                        until15 / 60, until15 % 60
                );
            } else {
                msg = "Текущая реклама перебита и может быть заменена в любой момент";
            }
        } else {
            msg = String.format(
                    "Осталось %02d:%02d до завершения показа текущей рекламы",
                    secondsLeft / 60, secondsLeft % 60
            );
        }
        return new AdTimeLeftResponse((int)(secondsLeft/60), (int)(secondsLeft%60), msg);
    }

    /**
     * Список ожидающих в очереди объявлений.
     */
    private List<Advertisement> getQueue() {
        return advertisementRepository.findAll().stream()
                .filter(ad -> ad.getStatus() == AdvertisementStatus.APPROVED)
                .filter(ad -> ad.getQueueStatus() == AdQueueStatus.WAITING)
                .filter(ad -> ad.getStartTime() == null)
                .sorted(Comparator.comparingInt(Advertisement::getCost).reversed()
                        .thenComparing(Advertisement::getCreatedAt))
                .collect(Collectors.toList());
    }

    /**
     * Текущий лидер (если есть).
     */
    private Optional<Advertisement> findCurrentLeader() {
        return advertisementRepository.findAll().stream()
                .filter(ad -> ad.getStatus() == AdvertisementStatus.APPROVED)
                .filter(ad -> ad.getQueueStatus() == AdQueueStatus.LEADING)
                .findFirst();
    }

    /**
     * Отменяет все запланированные задачи (и часовой, и 15‑минутный).
     */
    private void cancelPendingTasks() {
        cancelReplacementTask();
        if (currentLeaderTask != null) {
            currentLeaderTask.cancel(false);
            currentLeaderTask = null;
        }
    }

    private void cancelReplacementTask() {
        if (replacementTask != null) {
            replacementTask.cancel(false);
            replacementTask = null;
            pendingReplacement = null;
        }
    }

    /**
     * Сброс очереди в начальное состояние: все WAITING, без времени старта, без задач.
     */
    public void resetAdQueue() {
        lock.lock();
        try {
            cancelPendingTasks();
            advertisementRepository.findAll().forEach(ad -> {
                ad.setQueueStatus(AdQueueStatus.WAITING);
                ad.setStartTime(null);
            });
            advertisementRepository.saveAll(advertisementRepository.findAll());
        } finally {
            lock.unlock();
        }
    }
}
