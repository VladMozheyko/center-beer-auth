package fr.mossaab.security.controller;


import com.pusher.rest.Pusher;
import fr.mossaab.security.entities.Message;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.repository.MessageRepository;
import fr.mossaab.security.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
@Tag(name = "Чат", description = "API для работы администратором с пользователями")
@RestController
@RequestMapping("/messages")
public class MessageController {

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;

    @Autowired
    public MessageController(UserRepository userRepository, MessageRepository messageRepository) {
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
    }

    // Метод для отправки сообщения
    @Operation(summary = "Отправка сообщения", description = "Отправка сообщения пользователю по идентификатору")
    @PostMapping("/send/{userId}")
    public String sendMessage(@PathVariable Long userId, @RequestParam String messageContent) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        // Сохраняем сообщение в базе данных
        Message message = new Message();
        message.setUser(user);
        message.setMessageContent(messageContent);
        messageRepository.save(message);

        // Конфигурация Pusher
        Pusher pusher = new Pusher("1903493", "a14103e3348e2ae38302", "aa0b4bf2b5ae3668699d");
        pusher.setCluster("eu");
        pusher.setEncrypted(true);

        // Отправка сообщения пользователю через Pusher
        pusher.trigger("my-channel-" + userId, "my-event", Collections.singletonMap("message", messageContent));

        return "Message sent to user " + user.getFirstname() + " " + user.getLastname();
    }

    // Метод для вывода всех сообщений для пользователя
    @Operation(summary = "Вывод всех сообщений", description = "Вывод всех сообщений отправленных пользователю найденному по идентификатору")
    @GetMapping("/history/{userId}")
    public MessageHistoryResponseDTO getMessagesHistory(@PathVariable Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        // Получаем все сообщения для данного пользователя
        List<Message> messages = messageRepository.findByUserId(userId);

        // Создаем объект UserDTO для пользователя
        UserDTO userDTO = new UserDTO(user.getFirstname(), user.getLastname(), user.getEmail());

        // Преобразуем все сообщения в список MessageDTO
        List<MessageDTO> messageDTOs = messages.stream()
                .map(message -> new MessageDTO(message.getMessageContent(), message.getCreatedAt().toString()))
                .collect(Collectors.toList());

        // Возвращаем объект с информацией о пользователе и его сообщениях
        return new MessageHistoryResponseDTO(userDTO, messageDTOs);
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class MessageDTO {
        private String messageContent;
        private String createdAt;
    }
    @Getter
    @Setter
    @AllArgsConstructor
    public static class MessageHistoryResponseDTO {
        private UserDTO user;
        private List<MessageDTO> messages;
    }
    @Getter
    @Setter
    @AllArgsConstructor
    public static class UserDTO {
        private String firstname;
        private String lastname;
        private String email;
    }
}
