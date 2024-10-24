package fr.mossaab.security.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import fr.mossaab.security.enums.RequestStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "DOCUMENT_VERIFICATION")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class DocumentVerificationRequest {

    /**
     * Уникальный идентификатор проверки.
     */
    @Id
    @GeneratedValue
    private Long id;

    /**
     * Статус верификации.
     */
    @Enumerated(EnumType.STRING)
    private RequestStatus status;

    /**
     * Список файлов, связанных с верификацией.
     */
    @OneToMany(mappedBy = "documentVerification", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<FileData> fileDataList;

    /**
     * Пользователь, подавший документы на верификацию.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "_user_id")
    @JsonBackReference
    private User user;

    @Column(length = 500)
    private String rejectionMessage;

    private LocalDateTime requestDate;

    /**
     * Дата ответа на запрос.
     */
    private LocalDateTime responseDate;

}
