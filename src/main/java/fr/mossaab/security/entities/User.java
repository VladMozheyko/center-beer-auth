package fr.mossaab.security.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import fr.mossaab.security.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static jakarta.persistence.FetchType.EAGER;


@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "_user")
public class User implements UserDetails {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, columnDefinition = "bigint default 0")
    private Integer temporarySecondsBalance = 0;

    @NotBlank(message = "Никнейм не может быть пустым.")
    @Size(min = 3, max = 50, message = "Никнейм должен содержать от 3 до 50 символов.")
    @Column(nullable = false, unique = true)
    private String nickname;

    @NotBlank(message = "Электронная почта не может быть пустой.")
    @Pattern(
            regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
            message = "Неверный формат электронной почты. Пример: «example@domain.com»."
    )
    @Column(nullable = false, unique = true)
    private String email;
    private String tempEmail;
    private String password;

    @Column(length = 20, unique = true)
    private String phone;

    @Column(length = 6)
    private String phoneActivationCode;

    private Boolean phoneVerified = false;

    private String activationCode;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ------------ социальные сети ------------

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<UserSocialAccount> socialAccounts = new HashSet<>();
    //------------------_____--------------------

    @OneToOne(mappedBy = "user", cascade = CascadeType.MERGE, orphanRemoval = true)
    @JsonManagedReference
    private FileData fileData;

    @Enumerated(EnumType.STRING)
    private Role role;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RefreshToken> refreshTokens;

    @OneToOne(cascade = CascadeType.ALL, fetch = EAGER)
    @JoinColumn(name = "location_id")
    private Location location;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return role.getAuthorities();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

}
