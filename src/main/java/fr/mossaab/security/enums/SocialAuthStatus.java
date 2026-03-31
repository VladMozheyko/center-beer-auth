package fr.mossaab.security.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Коды статусов social login")
public enum SocialAuthStatus {
    @Schema(description = "Аккаунт соцсети уже есть")
    SOCIAL_FOUND,
    @Schema(description = "Эта email соцсети уже используется в другой учётке")
    EMAIL_LINKED,
    @Schema(description = "Пользователь найден по email, socialId еще не привязан")
    NEW_SOCIAL_USER,
    @Schema(description = "Пользователь новый, можно создать аккаунт")
    NEW_ACCOUNT,
    @Schema(description = "Критическая ошибка")
    ERROR
}