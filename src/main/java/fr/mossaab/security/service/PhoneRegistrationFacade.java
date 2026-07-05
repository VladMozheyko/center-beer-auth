package fr.mossaab.security.service;

import fr.mossaab.security.builder.AuthenticationResponseBuilder;
import fr.mossaab.security.dto.UserIpTempDto;
import fr.mossaab.security.dto.auth.AuthenticationResponse;
import fr.mossaab.security.dto.auth.PhoneRegisterRequest;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.enums.Role;
import fr.mossaab.security.exception.DuplicateResourceException;
import fr.mossaab.security.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class PhoneRegistrationFacade {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PhoneVerificationService phoneVerificationService;
    private final UserIpTempService userIpTempService;
    private final AuthenticationResponseBuilder authenticationResponseBuilder;

    private String maskPhoneNumber(String phone) {
        if (phone == null || phone.length() < 4) {
            return "***";
        }
        String visiblePart = phone.substring(0, 4);
        String hiddenPart = phone.substring(4).replaceAll(".", "*");
        return visiblePart + hiddenPart;
    }

    @Transactional
    public void start(PhoneRegisterRequest dto, HttpServletRequest request) {
        String maskedPhone = maskPhoneNumber(dto.getPhone());
        log.info("[REGISTER_BY_PHONE] - Процесс регистрации через логин и пароль по телефону, телефон:{}", maskedPhone);

        // Проверка существования пользователя с таким же email и activationCode == null
        var existingUserByEmail = userRepository.findByEmail(dto.getEmail());
        if (existingUserByEmail.isPresent() && existingUserByEmail.get().getActivationCode() == null) {
            log.warn("[REGISTER_BY_PHONE] - Данный Email {} занят другим пользователем ", dto.getEmail());
            throw new DuplicateResourceException(
                    "Пользователь с таким email уже существует и активирован.",
                    "email_exists"
            );
        }

        var existingUserByNickname = userRepository.findByNickname(dto.getNickname());
        if (existingUserByNickname.isPresent() && existingUserByNickname.get().getActivationCode() == null) {
            log.warn("[REGISTER_BY_PHONE] - Пользователь с таким никнеймом уже существует и активирован nickname={}",
                    dto.getNickname());
            throw new DuplicateResourceException(
                    "Пользователь с таким никнеймом уже существует и активирован.",
                    "nickname_exists"
            );
        }

        var existingUserByPhone = userRepository.findByPhone(dto.getPhone());
        if (existingUserByPhone.isPresent() && existingUserByPhone.get().getActivationCode() == null) {
            log.warn("[REGISTER_BY_PHONE] - Пользователь с таким телефоном уже существует и активирован phone={}",
                    maskPhoneNumber(dto.getPhone()));
            throw new DuplicateResourceException(
                    "Пользователь с таким телефоном уже существует и активирован.",
                    "phone_exists"
            );
        }

        String code = phoneVerificationService.sendCode(dto.getPhone(), dto.getMethod());

        var user = User.builder()
                .email(dto.getEmail())
                .nickname(dto.getNickname())
                .password(passwordEncoder.encode(dto.getPassword()))
                .phone(dto.getPhone())
                .role(Role.USER)
                .temporarySecondsBalance(0)
                .activationCode(code)
                .build();

        try {
            userRepository.save(user);
            log.info("[REGISTER_BY_PHONE] Пользователь {} сохранен ", user.getEmail());
        } catch (Exception e) {
            log.error("[BASE REGISTER] - Ошибка при сохранении пользователя {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }

        String ip = authenticationResponseBuilder.getIpHelper().getClientIp(request);
        userIpTempService.saveIpTemp(user.getId(), ip);
    }

    @Transactional
    public void confirm(String phone, String code) {
        User u = userRepository.findByPhone(phone)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!Objects.equals(code, u.getPhoneActivationCode())) {
            throw new IllegalArgumentException("Wrong code");
        }
        u.setPhoneActivationCode(null);
        u.setPhoneVerified(true);
    }
}