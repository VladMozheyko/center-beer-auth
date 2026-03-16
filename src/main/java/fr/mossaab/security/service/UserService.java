package fr.mossaab.security.service;

import fr.mossaab.security.entities.User;
import fr.mossaab.security.exception.DuplicateResourceException;
import fr.mossaab.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean confirmEmailChange(String code) {
        logger.info("Попытка подтверждения email для кода: {}", code);
        
        User user = userRepository.findByActivationCode(code)
                .orElseThrow(() -> {
                    logger.warn("Код не найден в базе: {}", code);
                    return new IllegalArgumentException("Неверный код подтверждения");
                });

        logger.info("Пользователь найден: id={}, email={}, tempEmail={}", 
                user.getId(), user.getEmail(), user.getTempEmail());

        if (user.getTempEmail() == null || user.getTempEmail().isEmpty()) {
            logger.warn("tempEmail пуст для пользователя: {}", user.getId());
            throw new IllegalStateException("Отсутствует новый e-mail для изменения");
        }

        // Проверяем, не занят ли новый адрес
        if (userRepository.findByEmail(user.getTempEmail()).isPresent()) {
            logger.warn("Email уже занят: {}", user.getTempEmail());
            throw new DuplicateResourceException("Данный e-mail уже используется другим пользователем", "email_exists");
        }

        String updatedEmail = user.getTempEmail();
        user.setEmail(updatedEmail);
        user.setTempEmail(null);
        user.setActivationCode(null);
        userRepository.saveAndFlush(user);
        
        logger.info("Email успешно изменён на: {}", updatedEmail);

        return true;
    }

    public String getNewEmailForConfirmation(String code) {
        return userRepository.findByActivationCode(code)
                .map(User::getTempEmail)
                .orElse(null);
    }

    @Transactional
    public void deleteUser(Long userId) {
        logger.info("Удаление пользователя с id: {}", userId);
        if (!userRepository.existsById(userId)) {
            logger.warn("Пользователь с id {} не найден", userId);
            throw new IllegalArgumentException("Пользователь не найден");
        }
        userRepository.deleteById(userId);
        logger.info("Пользователь с id {} успешно удалён", userId);
    }
}
