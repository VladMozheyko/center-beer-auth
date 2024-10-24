package fr.mossaab.security.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import fr.mossaab.security.enums.Role;
import fr.mossaab.security.enums.WorkerRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Сущность пользователя, реализующая интерфейс UserDetails для интеграции с Spring Security.
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "_user")
public class User implements UserDetails {

    /**
     * Уникальный идентификатор пользователя.
     */
    @Id
    @GeneratedValue
    private Long id;

    /**
     * Имя пользователя.
     */
    private String firstname;

    /**
     * Фамилия пользователя.
     */
    private String lastname;

    /**
     * Электронная почта пользователя.
     */
    private String email;

    /**
     * Пароль пользователя.
     */
    private String password;

    /**
     * Код активации пользователя.
     */
    private String activationCode;

    /**
     * Номер телефона пользователя.
     */
    private String phoneNumber;
    @Column(nullable = false, columnDefinition = "bigint default 0")
    private int balance = 0;
    /**
     * Данные файла, связанные с пользователем.
     */
    @OneToOne(mappedBy = "user", cascade = CascadeType.MERGE, orphanRemoval = true)
    //@JoinColumn(name = "file_data_id", referencedColumnName = "id")
    @JsonManagedReference
    private FileData fileData;

    /**
     * Роли работника.
     */
    @Enumerated(EnumType.STRING)
    private WorkerRole workerRoles;

    /**
     * Дата рождения пользователя.
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Temporal(TemporalType.DATE)
    private Date dateOfBirth;

    /**
     * Роль пользователя.
     */
    @Enumerated(EnumType.STRING)
    private Role role;
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<BonusRequest> bonusRequests;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<RefreshToken> refreshTokens;
    @Embedded
    private ProposedChanges proposedChanges;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<DocumentVerificationRequest> documentVerifications;
    /**
     * Получение списка ролей пользователя.
     *
     * @return Список ролей пользователя.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return role.getAuthorities();
    }

    /**
     * Получение пароля пользователя.
     *
     * @return Пароль пользователя.
     */
    @Override
    public String getPassword() {
        return password;
    }

    /**
     * Получение имени пользователя.
     *
     * @return Имя пользователя.
     */
    @Override
    public String getUsername() {
        return email;
    }

    /**
     * Проверка на истечение срока действия учетной записи.
     *
     * @return Всегда возвращает true.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Проверка на блокировку учетной записи пользователя.
     *
     * @return Всегда возвращает true.
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Проверка учетных данных пользователя на истечение срока действия.
     *
     * @return Всегда возвращает true.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Проверка, активен ли пользователь.
     *
     * @return Всегда возвращает true.
     */
    @Override
    public boolean isEnabled() {
        return true;
    }
}
