package fr.mossaab.security.config;

import fr.mossaab.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// Аннотация @Configuration указывает, что данный класс содержит определения бинов и должен управляться контейнером Spring.
@Configuration
// Аннотация @RequiredArgsConstructor генерирует конструктор с требуемыми аргументами для всех final полей.
@RequiredArgsConstructor
public class ApplicationSecurityConfig {
    // Репозиторий для доступа к пользователям.
    private final UserRepository userRepository;

    /**
     * Определение бина UserDetailsService. Этот бин используется для загрузки данных пользователя на основе имени пользователя.
     * Если пользователь с заданным именем пользователя не найден, выбрасывается исключение UsernameNotFoundException.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    /**
     * Определение бина AuthenticationProvider. Этот бин отвечает за проверку подлинности пользователей.
     * DaoAuthenticationProvider используется для получения информации о пользователе и проверки его пароля.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        // Создаем экземпляр DaoAuthenticationProvider.
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        // Устанавливаем UserDetailsService для получения информации о пользователе.
        authProvider.setUserDetailsService(userDetailsService());
        // Устанавливаем PasswordEncoder для проверки пароля.
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Определение бина AuthenticationManager. Этот бин используется для управления процессом аутентификации.
     * @param config Параметр конфигурации аутентификации.
     * @return Возвращает экземпляр AuthenticationManager.
     * @throws Exception Если возникает ошибка при получении AuthenticationManager.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        // Возвращаем AuthenticationManager из конфигурации.
        return config.getAuthenticationManager();
    }

    /**
     * Определение бина PasswordEncoder. Этот бин используется для кодирования паролей.
     * @return Возвращает экземпляр BCryptPasswordEncoder, который использует алгоритм BCrypt для хеширования паролей.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // Возвращаем новый экземпляр BCryptPasswordEncoder.
        return new BCryptPasswordEncoder();
    }
}