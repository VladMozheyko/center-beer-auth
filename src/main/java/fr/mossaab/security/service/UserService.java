package fr.mossaab.security.service;

import fr.mossaab.security.dto.FileDataDto;
import fr.mossaab.security.dto.social.SocialAccountResponse;
import fr.mossaab.security.dto.user.AllUserInfoResponse;
import fr.mossaab.security.entities.FileData;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.entities.UserSocialAccount;
import fr.mossaab.security.exception.DuplicateResourceException;
import fr.mossaab.security.repository.FileDataRepository;
import fr.mossaab.security.repository.UserRepository;
import fr.mossaab.security.repository.UserSocialAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final FileDataRepository fileDataRepository;
    private final UserSocialAccountRepository userSocialAccountRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean confirmEmailChange(String code) {
        log.info("Попытка подтверждения email для кода: {}", code);
        
        User user = userRepository.findByActivationCode(code)
                .orElseThrow(() -> {
                    log.warn("Код не найден в базе: {}", code);
                    return new IllegalArgumentException("Неверный код подтверждения");
                });

        log.info("Пользователь найден: id={}, email={}, tempEmail={}",
                user.getId(), user.getEmail(), user.getTempEmail());

        if (user.getTempEmail() == null || user.getTempEmail().isEmpty()) {
            log.warn("tempEmail пуст для пользователя: {}", user.getId());
            throw new IllegalStateException("Отсутствует новый e-mail для изменения");
        }

        // Проверяем, не занят ли новый адрес
        if (userRepository.findByEmail(user.getTempEmail()).isPresent()) {
            log.warn("Email уже занят: {}", user.getTempEmail());
            throw new DuplicateResourceException("Данный e-mail уже используется другим пользователем", "email_exists");
        }

        String updatedEmail = user.getTempEmail();
        user.setEmail(updatedEmail);
        user.setTempEmail(null);
        user.setActivationCode(null);
        userRepository.saveAndFlush(user);
        
        log.info("Email успешно изменён на: {}", updatedEmail);

        return true;
    }

    public String getNewEmailForConfirmation(String code) {
        return userRepository.findByActivationCode(code)
                .map(User::getTempEmail)
                .orElse(null);
    }

    @Transactional
    public void deleteUser(Long userId) {
        log.info("Удаление пользователя с id: {}", userId);
        if (!userRepository.existsById(userId)) {
            log.warn("Пользователь с id {} не найден", userId);
            throw new IllegalArgumentException("Пользователь не найден");
        }
        userRepository.deleteById(userId);
        log.info("Пользователь с id {} успешно удалён", userId);
    }

    public AllUserInfoResponse getFullUserInfo(Long id){
        log.info("[FULL_INFO] Получение полной информации о пользователе");
        log.debug("[FULL_INFO] Получение пользователя по id:{}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        log.debug("[FULL_INFO] Получение fileData для пользователя с id:{}", id);
        Optional<FileData> file = fileDataRepository.findByUserId(user.getId());

        FileDataDto fileResponse = new FileDataDto();
        if (file.isPresent()){
            FileData fileData = file.get();
            fileResponse.setName(fileData.getName());
            fileResponse.setPath(fileData.getFilePath());
            log.debug("[FULL_INFO] FileData найден: {}", fileResponse);
        }

        log.debug("[FULL_INFO] Получение socialAccounts для пользователя с id:{}", id);
        List<UserSocialAccount> socialAccounts = userSocialAccountRepository.findByUserId(id);
        List<SocialAccountResponse> socialAccountsResponse = new ArrayList<>();
        if (!socialAccounts.isEmpty()){
            socialAccounts.forEach(socialAccount -> {
                SocialAccountResponse response = new SocialAccountResponse();
                response.setProvider(socialAccount.getProvider());
                response.setSocialEmail(socialAccount.getSocialEmail());
                socialAccountsResponse.add(response);
            });
            log.debug("[FULL_INFO] socialAccounts найдены: {}", socialAccountsResponse);
        }

        log.debug("[FULL_INFO] Создаем AllUserInfoResponse для пользователя с id:{}", id);
        AllUserInfoResponse response = new AllUserInfoResponse();
        response.setUserName(user.getNickname());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setFileData(fileResponse);
        response.setSocialAccounts(socialAccountsResponse);

        log.debug("[FULL_INFO] Создан {} для пользователя с id:{}", response, id);
        log.info("[FULL_INFO] Получен AllUserInfoResponse для пользователя с id:{}", id);
        return response;
    }
}
