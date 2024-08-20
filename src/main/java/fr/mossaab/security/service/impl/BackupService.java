package fr.mossaab.security.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.mossaab.security.dto.request.FileDataDTO;
import fr.mossaab.security.dto.request.RefreshTokenDTO;
import fr.mossaab.security.dto.request.UserBackupDTO;
import fr.mossaab.security.entities.RefreshToken;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BackupService implements ApplicationListener<ContextClosedEvent> {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private static final String BACKUP_FILE_PATH = "/var/www/vuary/user_backup/users_backup.json";

    //private static final String BACKUP_FILE_PATH = "C:/Users/Admin/Desktop/kotitonttu1207/src/main/resources/users_backup.json";
    @Override
    @Transactional(readOnly = true)
    public void onApplicationEvent(ContextClosedEvent event) {
        backupUsers();
    }

    private void backupUsers() {
        List<User> users = userRepository.findAll(); // Загружаем всех пользователей

        // Инициализация всех ленивых коллекций
        initializeUsers(users);

        List<UserBackupDTO> userDTOs = users.stream().map(this::toDTO).toList();

        try {
            objectMapper.writeValue(new File(BACKUP_FILE_PATH), userDTOs);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initializeUsers(List<User> users) {
        for (User user : users) {
            // Инициализируем ленивые коллекции
            Hibernate.initialize(user.getRefreshTokens());
            if (user.getFileData() != null) {
                Hibernate.initialize(user.getFileData());
            }
        }
    }

    private UserBackupDTO toDTO(User user) {
        UserBackupDTO userDTO = new UserBackupDTO();
        userDTO.setId(user.getId());
        userDTO.setFirstname(user.getFirstname());
        userDTO.setLastname(user.getLastname());
        userDTO.setEmail(user.getEmail());
        userDTO.setPhoneNumber(user.getPhoneNumber());
        userDTO.setWorkerRoles(user.getWorkerRoles());
        userDTO.setDateOfBirth(user.getDateOfBirth());
        userDTO.setRole(user.getRole());
        userDTO.setActivationCode(user.getActivationCode());

        if (user.getFileData() != null) {
            FileDataDTO fileDataDTO = new FileDataDTO();
            fileDataDTO.setId(user.getFileData().getId());
            fileDataDTO.setName(user.getFileData().getName());
            fileDataDTO.setType(user.getFileData().getType());
            fileDataDTO.setFilePath(user.getFileData().getFilePath());
            userDTO.setFileData(fileDataDTO);
        }

        // Маппинг токенов
        List<RefreshTokenDTO> tokenDTOs = user.getRefreshTokens().stream().map(this::toTokenDTO).toList();
        userDTO.setRefreshTokens(tokenDTOs);

        return userDTO;
    }

    private RefreshTokenDTO toTokenDTO(RefreshToken token) {
        RefreshTokenDTO tokenDTO = new RefreshTokenDTO();
        tokenDTO.setId(token.getId());
        tokenDTO.setToken(token.getToken());
        tokenDTO.setExpiryDate(token.getExpiryDate());
        tokenDTO.setRevoked(token.isRevoked());
        return tokenDTO;
    }
}
