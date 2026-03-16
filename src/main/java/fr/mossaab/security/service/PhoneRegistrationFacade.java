package fr.mossaab.security.service;

import fr.mossaab.security.dto.auth.PhoneRegisterRequest;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.enums.Role;
import fr.mossaab.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PhoneRegistrationFacade {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PhoneVerificationService phoneVerificationService;

    @Transactional
    public void start(PhoneRegisterRequest dto) {

        var user = User.builder()
                .email(dto.getEmail())
                .nickname(dto.getNickname())
                .password(passwordEncoder.encode(dto.getPassword()))
                .phone(dto.getPhone())
                .role(Role.USER)
                .temporarySecondsBalance(0)
                .build();

        // *** Сохраняем, чтобы получить id ***
        userRepository.save(user);

        // *** Отправляем код выбранным методом ***
        String code = phoneVerificationService.sendCode(dto.getPhone(), dto.getMethod());
        user.setPhoneActivationCode(code);
        userRepository.save(user);
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