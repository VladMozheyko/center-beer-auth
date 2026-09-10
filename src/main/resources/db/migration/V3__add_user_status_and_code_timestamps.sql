-- Добавляем статус активации и время отправки кода
ALTER TABLE _user ADD COLUMN status VARCHAR(20) DEFAULT 'PENDING' NOT NULL;
ALTER TABLE _user ADD COLUMN last_code_sent_at TIMESTAMP NULL;

-- Миграция существующих данных: активированные = activation_code IS NULL
UPDATE _user SET status = 'ACTIVATED' WHERE activation_code IS NULL;
UPDATE _user SET status = 'PENDING' WHERE activation_code IS NOT NULL AND status = 'PENDING';

-- Индекс для оптимизации очистки просроченных записей
CREATE INDEX idx_user_status_created_at ON _user(status, created_at);
