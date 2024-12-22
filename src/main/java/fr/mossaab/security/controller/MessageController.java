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
    @Operation(summary = "Отправка сообщения", description = "Отправка сообщения от одного пользователя другому")
    @PostMapping("/send")
    public String sendMessage(
            @RequestParam Long senderId,
            @RequestParam Long receiverId,
            @RequestParam String messageContent) {

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Отправитель не найден."));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Получатель не найден."));

        // Сохраняем сообщение
        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setMessageContent(messageContent);
        messageRepository.save(message);

        // Конфигурация Pusher
        Pusher pusher = new Pusher("1903493", "a14103e3348e2ae38302", "aa0b4bf2b5ae3668699d");
        pusher.setCluster("eu");
        pusher.setEncrypted(true);

        // Отправка сообщения через Pusher
        pusher.trigger("my-channel-" + receiverId, "my-event", Collections.singletonMap("message", messageContent));

        return "Message sent from " + sender.getEmail() + " to " + receiver.getEmail();
    }


    @Operation(summary = "Получить все диалоги пользователя", description = "Получить все сообщения, где пользователь участвует как отправитель или получатель")
    @GetMapping("/dialogues/{userId}")
    public List<DialogueDTO> getUserDialogues(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Получаем все сообщения, где пользователь отправитель или получатель
        List<Message> messages = messageRepository.findBySenderIdOrReceiverIdOrderByCreatedAt(userId, userId);

        // Группируем сообщения по собеседнику
        return messages.stream()
                .collect(Collectors.groupingBy(message -> {
                    // Определяем собеседника
                    return message.getSender().getId().equals(userId)
                            ? message.getReceiver()
                            : message.getSender();
                }))
                .entrySet().stream()
                .map(entry -> {
                    User interlocutor = entry.getKey();
                    List<MessageDTO> messageDTOs = entry.getValue().stream()
                            .map(msg -> new MessageDTO(
                                    msg.getMessageContent(),
                                    msg.getCreatedAt().toString(),
                                    msg.getSender().getId().equals(userId) ? "outgoing" : "incoming"))
                            .collect(Collectors.toList());
                    return new DialogueDTO(
                            new UserDTO(interlocutor.getEmail(), interlocutor.getEmail(), interlocutor.getEmail()),
                            messageDTOs
                    );
                })
                .collect(Collectors.toList());
    }


    @Getter
    @Setter
    @AllArgsConstructor
    public static class MessageDTO {
        private String messageContent;
        private String createdAt;
        private String direction; // "incoming" или "outgoing"
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class DialogueDTO {
        private UserDTO interlocutor; // Собеседник
        private List<MessageDTO> messages; // Сообщения
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
