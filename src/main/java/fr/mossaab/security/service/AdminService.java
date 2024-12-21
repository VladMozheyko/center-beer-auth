package fr.mossaab.security.service;

import fr.mossaab.security.entities.User;
import fr.mossaab.security.enums.Role;
import fr.mossaab.security.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class AdminService {
    private UserRepository userRepository;
    private static final String LOG_FILE_PATH = "/root/kotitonttu/log.txt";

    public String getLogs() throws IOException {
        Path logPath = Path.of(LOG_FILE_PATH);
        return Files.readString(logPath);
    }
    public GetAllUsersResponse getAllUsers(int page, int size) {
        List<User> users = userRepository.findAll();
        List<GetUsersDto> userDtos = new ArrayList<>();

        for (User user : users) {
            Role role = user.getRole();
            AdminService.GetUsersDto userDto = new AdminService.GetUsersDto(
                    user.getFirstname() != null ? user.getFirstname() : null,
                    user.getEmail() != null ? user.getEmail() : null,
                    user.getLastname() != null ? user.getLastname() : null,
                    user.getPhoneNumber() != null ? user.getPhoneNumber() : null,
                    user.getDateOfBirth() != null ? user.getDateOfBirth().toString() : null,
                    "http://31.129.102.70:8080/user/fileSystem/" + (user.getFileData() != null && user.getFileData().getName() != null ? user.getFileData().getName() : null),
                    user.getActivationCode() == null,
                    role != null ? role.name() : null,
                    user.getId() != null ? user.getId().toString() : null,
                    user.getBalance()
            );
            userDtos.add(userDto);
        }

        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, userDtos.size());
        List<GetUsersDto> paginatedUserDtos = userDtos.subList(startIndex, endIndex);

        GetAllUsersResponse response = new GetAllUsersResponse();
        response.setStatus("success");
        response.setNotify("Пользователи получены");
        response.setUsers(paginatedUserDtos);
        response.setOffset(page + 1);
        response.setPageNumber(page);
        response.setTotalElements(userDtos.size());
        response.setTotalPages((int) Math.ceil((double) userDtos.size() / size));
        response.setPageSize(size);
        response.setLast((page + 1) * size >= userDtos.size());
        response.setFirst(page == 0);

        return response;
    }
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GetAllUsersResponse {

        private String status;
        private String notify;
        private List<GetUsersDto> users;
        private int offset;
        private int pageNumber;
        private long totalElements;
        private int totalPages;
        private int pageSize;
        private boolean last;
        private boolean first;

    }
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class GetUsersDto {

        /**
         * Имя пользователя.
         */
        private String firstname;

        /**
         * Электронная почта пользователя.
         */
        private String email;

        /**
         * Фамилия пользователя.
         */
        private String lastname;

        /**
         * Номер телефона пользователя.
         */
        private String phoneNumber;

        /**
         * Дата рождения пользователя.
         */
        private String dateOfBirth;

        /**
         * Фотография пользователя.
         */
        private String photo;

        /**
         * Код активации.
         */
        private Boolean activationCode;

        /**
         * Роль пользователя.
         */
        private String role;

        /**
         * Уникальный идентификатор пользователя.
         */
        private String id;
        private int balance;

    }
}
