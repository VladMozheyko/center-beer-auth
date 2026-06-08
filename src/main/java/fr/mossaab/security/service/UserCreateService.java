package fr.mossaab.security.service;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.enums.Role;
import fr.mossaab.security.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@AllArgsConstructor
public class UserCreateService {

    private final UserRepository userRepository;

    public void createUsers() {
        if (userRepository.count() == 0) {
            createUser(501L, "Vlad72229@yandex.ru",
                    "$2a$10$QGl4Wtd20zVUu3BRYqBs5uGCsWDE0rvabE2I/XBWxQl0/NOdGwILS",null,Role.ADMIN);
        }
    }

    private void createUser(Long id,String email,
                            String password, String activationCode, Role role) {
        try {

            User user = User.builder()
                    .temporarySecondsBalance(0)
                    .tempEmail(null)
                    .nickname("Vlad72229@yandex.ru")
                    .id(id) // Предполагается, что id уже задан
                    .email(email)
                    .password(password) // Пароль уже зашифрован
                    .role(role)
                    .activationCode(activationCode)
                    .createdAt(LocalDateTime.now())
                    .build();
            log.debug(">>>> User {} created, ROLE {}", user.getEmail(), user.getRole());
            userRepository.save(user);
        } catch (Exception e) {
            log.warn("User {} is not created", email);
            // Обработка ошибок (например, вывод в лог)
            e.printStackTrace();
        }
    }
}
