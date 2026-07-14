package fr.mossaab.security.controller;


import fr.mossaab.security.dto.UserIpTempDto;
import fr.mossaab.security.dto.user.AllUserInfoResponse;
import fr.mossaab.security.dto.user.LocationDto;
import fr.mossaab.security.dto.user.UserProfileResponse;
import fr.mossaab.security.entities.*;
import fr.mossaab.security.exception.DuplicateResourceException;
import fr.mossaab.security.repository.*;
import fr.mossaab.security.service.MailSender;
import fr.mossaab.security.service.UserIpTempService;
import fr.mossaab.security.service.UserService;
import fr.mossaab.security.validation.annotation.ValidEmail;
import fr.mossaab.security.validation.annotation.ValidRuEnNicknameLengthMin4Max50;
import fr.mossaab.security.validation.annotation.ValidSmsCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Пользователь", description = "Контроллер предоставляет базовые методы доступные пользователю с ролью user")
@RestController
@RequestMapping("/user")
@SecurityRequirements()
@RequiredArgsConstructor
@Validated
public class UserController {
    private final UserRepository userRepository;
    private final MailSender mailSender;
    private final UserService userService;
    private final LocationRepository locationRepository;
    private final UserIpTempService userIpTempService;

    @Value("${app.server.base-url:https://api.center.beer/auth_service}")
    private String publicUrl;

    @Operation(summary = "Получить профиль пользователя")
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getSimpleProfile() {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        UserProfileResponse profile = UserProfileResponse.builder()
                .id(user.getId()) // <-- добавлено
                .nickname(user.getNickname())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .country(user.getLocation() != null ? user.getLocation().getCountry() : null)
                .city(user.getLocation() != null ? user.getLocation().getCity() : null)
                .latitude(user.getLocation() != null ? user.getLocation().getLatitude() : null)
                .longitude(user.getLocation() != null ? user.getLocation().getLongitude() : null)
                .build();

        return ResponseEntity.ok(profile);
    }

    @Operation(summary = "Изменить никнейм текущего пользователя")
    @PatchMapping("/update-nickname")
    public ResponseEntity<String> updateNickname(@RequestParam @ValidRuEnNicknameLengthMin4Max50 String newNickname) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Проверяем, не занят ли новый никнейм другим пользователем
        var existingUserByNickname = userRepository.findByNickname(newNickname);
        if (existingUserByNickname.isPresent() 
                && !existingUserByNickname.get().getId().equals(user.getId())
                && existingUserByNickname.get().getActivationCode() == null) {
            throw new DuplicateResourceException("Пользователь с таким никнеймом уже существует", "nickname_exists");
        }

        user.setNickname(newNickname);
        userRepository.save(user);
        return ResponseEntity.ok("Никнейм успешно обновлён на " + newNickname);
    }

    @Operation(summary = "Запросить смену e-mail (отправляет ссылку на новый адрес)")
    @PostMapping("/request-email-change")
    @Transactional
    public ResponseEntity<String> requestEmailChange(@RequestParam @ValidEmail String newEmail) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        String activationCode = UUID.randomUUID().toString();
        user.setActivationCode(activationCode);
        user.setTempEmail(newEmail);
        userRepository.saveAndFlush(user);

        String confirmLink = publicUrl + "/user/confirm-email-change?code=" + activationCode;
        String message = "Здравствуйте! Перейдите по ссылке для подтверждения: \n" + confirmLink;
        mailSender.send(newEmail, "Подтверждение смены e-mail", message);

        return ResponseEntity.ok("Ссылка для подтверждения отправлена на " + newEmail);
    }

    @Operation(summary = "Подтвердить смену e-mail")
    @GetMapping("/confirm-email-change")
    public ResponseEntity<String> confirmEmailChange(@RequestParam @ValidSmsCode String code) {
        try {
            userService.confirmEmailChange(code);
            return ResponseEntity.ok("E-mail успешно изменён");
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (DuplicateResourceException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Ошибка при подтверждении: " + e.getMessage());
        }
    }

    @Operation(summary = "Удалить аккаунт текущего пользователя")
    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteAccount() {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        userService.deleteUser(user.getId());
        return ResponseEntity.ok("Аккаунт успешно удалён");
    }

    @Operation(summary = "Сохранить геолокацию", description = "Сохранение геолокации пользователя")
    @PostMapping("/profile/location")
    public ResponseEntity<?> updateMyLocation(@Valid @RequestBody LocationDto location) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        Location loc = Location.builder()
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .country(location.getCountry())
                .city(location.getCity())
                .build();
        if (user.getLocation() != null) {
            Location oldLocation = user.getLocation();
            user.setLocation(null);
            userRepository.save(user);
            locationRepository.delete(oldLocation);
        }
        user.setLocation(loc);
        userRepository.save(user);
        return ResponseEntity.ok("Геолокация успешно сохранена longitude: " + loc.getLongitude() + " / latitude: " + loc.getLatitude());
    }

    @Operation(summary = "Получить последние IPs пользователя", description = "Получение всех ip c которых пользователь совершал регистрации")
    @GetMapping("/{id}/registration/ips")
    public ResponseEntity<List<UserIpTempDto>> getListIpForUser(@PathVariable Long id) {
        List<UserIpTempDto> listIPs = userIpTempService.getTrackedIpForUser(id);
        return ResponseEntity.ok(listIPs);
    }

    @Operation(
            summary = "Получить профиль по id",
            description = "Получение всех данных о пользователе")
    @GetMapping("/profile/{id}")
    public ResponseEntity<AllUserInfoResponse> getProfile(
            @Valid @PathVariable Long id) {
        AllUserInfoResponse fullInfo = userService.getFullUserInfo(id);

        return ResponseEntity.ok(fullInfo);
    }
}
