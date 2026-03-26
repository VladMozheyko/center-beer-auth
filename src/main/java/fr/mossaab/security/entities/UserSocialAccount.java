package fr.mossaab.security.entities;

import fr.mossaab.security.enums.OAuthProvider;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "user_social_account")
public class UserSocialAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private OAuthProvider provider; // VK, GOOGLE, YANDEX...

    @Column(unique = true, nullable = false)
    private String externalId; // vkId, googleSub, yandexUid

    @Column(nullable = true)
    private String socialEmail; // email, если был отдан соцсетью
}
