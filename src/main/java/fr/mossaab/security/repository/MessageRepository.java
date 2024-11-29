package fr.mossaab.security.repository;

import fr.mossaab.security.entities.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    // Получить все сообщения для пользователя
    List<Message> findByUserId(Long userId);
}
