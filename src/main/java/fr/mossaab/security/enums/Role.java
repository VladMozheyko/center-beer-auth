package fr.mossaab.security.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static fr.mossaab.security.enums.Privilege.*;

@RequiredArgsConstructor
public enum Role {
    ADMIN,
    USER,
    ANONYMOUS;

    /**
     * Возвращает список SimpleGrantedAuthority, содержащий только роль.
     */
    public List<SimpleGrantedAuthority> getAuthorities() {
        // Добавляем только роль в формате ROLE_
        return List.of(new SimpleGrantedAuthority("ROLE_" + this.name()));
    }
}