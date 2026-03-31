package fr.mossaab.security.enums;

public enum OAuthProvider {
    VK,
    GOOGLE,
    YANDEX,
    DEFAULT; // обычная регистрация/аутентификация

    public static OAuthProvider fromString(String providerName) {
        try {
            return valueOf(providerName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
