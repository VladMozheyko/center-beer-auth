package fr.mossaab.security.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
// DTO для передчи данных запроса на смену пароля от клиента на сервер
// используем в методе класса AuthController
public class ChangePasswordRequest {
    private String currentPassword;
    private String newPassword;
}
