package fr.mossaab.security.enums;

public enum AdQueueStatus {
    WAITING,     // В очереди (после одобрения) (в ожидание лидерства)
    LEADING,     // Сейчас отображается (лидер)
    COMPLETED    // Уже была показана (была лидером)
}