package fr.mossaab.security.service;

import fr.mossaab.security.dto.response.GetAllUsersResponse;
import fr.mossaab.security.dto.response.GetUsersDto;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.enums.Role;
import fr.mossaab.security.enums.WorkerRole;
import fr.mossaab.security.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class AdminService {
    private UserRepository userRepository;

    public GetAllUsersResponse getAllUsers(int page, int size) {
        List<User> users = userRepository.findAll();
        List<GetUsersDto> userDtos = new ArrayList<>();

        for (User user : users) {
            WorkerRole workerRole = user.getWorkerRoles();
            Role role = user.getRole();
            GetUsersDto userDto = new GetUsersDto(
                    user.getFirstname() != null ? user.getFirstname() : null,
                    user.getEmail() != null ? user.getEmail() : null,
                    user.getLastname() != null ? user.getLastname() : null,
                    user.getPhoneNumber() != null ? user.getPhoneNumber() : null,
                    workerRole != null ? workerRole.name() : null,
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
}
