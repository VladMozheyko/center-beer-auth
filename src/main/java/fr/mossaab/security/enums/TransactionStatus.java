package fr.mossaab.security.enums;

public enum TransactionStatus {
    PENDING,    // Транзакция инициирована, ожидает завершения
    COMPLETED,  // Транзакция успешно завершена
    FAILED,     // Транзакция не удалась
    CANCELED    // Транзакция отменена
}
