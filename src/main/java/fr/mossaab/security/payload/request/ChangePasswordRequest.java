package fr.mossaab.security.payload.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
// DTO для передчи данных запроса на смену пароля от клиента на сервер
// используем в методе класса AuthenticationController
public class ChangePasswordRequest {
    private String currentPassword;
    private String newPassword;
}
