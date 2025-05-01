package fr.mossaab.security.config;

import fr.mossaab.security.service.OAuth2LoginSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;
import org.springframework.web.cors.CorsConfigurationSource;
/**
 * Конфигурационный класс SecurityConfiguration для настройки Spring Security.
 */
//@Configuration
//@EnableWebSecurity
//@RequiredArgsConstructor
//@EnableMethodSecurity
//public class SecurityConfiguration {
//    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
//    private static final Long MAX_AGE = 3600L; // Максимальное время жизни CORS-заголовков
//    private static final int CORS_FILTER_ORDER = -102; // Порядок CORS-фильтра
//    private final JwtAuthenticationFilter jwtAuthenticationFilter; // Фильтр аутентификации по JWT
//    private final AuthenticationProvider authenticationProvider; // Провайдер аутентификации
//
//    /**
//     * Конфигурация цепочки фильтров безопасности.
//     */
////    @Bean
////    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
////        return http
////                .csrf(AbstractHttpConfigurer::disable)
////                .authorizeHttpRequests(request -> request
////                        .requestMatchers(
////                                "/authentication/**",
////                                "/user/**",
////                                "/admin/**",
////                                "/v2/api-docs",
////                                "/v3/api-docs",
////                                "/v3/api-docs/**",
////                                "/swagger-resources",
////                                "/swagger-resources/**",
////                                "/configuration/ui",
////                                "/configuration/security",
////                                "/swagger-ui/**",
////                                "/webjars/**",
////                                "/swagger-ui.html",
////                                "/login/**",
////                                "/oauth2/**", // Важно для OAuth2
////                                "/login/oauth2/code/google"
////                        ).permitAll()
////                        .requestMatchers(HttpMethod.POST, "/api/v1/resource").hasRole("ADMIN")
////                        .anyRequest().authenticated()
////                )
////                .sessionManagement(manager -> manager.sessionCreationPolicy(STATELESS))
////                .authenticationProvider(authenticationProvider)
////                .oauth2Login(oauth2 -> oauth2
////                        .successHandler(oAuth2LoginSuccessHandler) // <-- передаём наш кастомный handler
////                )
////                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
////                .build();
////    }
//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http
//                // 1) CSRF off, сессии у нас stateless
//                .csrf(AbstractHttpConfigurer::disable)
//                .sessionManagement(m -> m.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
//
//                // 2) Отключаем стандартную форму логина
//                .formLogin(AbstractHttpConfigurer::disable)
//
//                // 3) Настраиваем OAuth2 login (чтобы работало /oauth2/authorization/{registrationId})
//                .oauth2Login(oauth2 -> oauth2
//                        // если вы хотите задать свой «начальный» URL для старта OAuth-потока:
//                        .loginPage("/oauth2/authorization/vk")
//                        .successHandler(oAuth2LoginSuccessHandler)
//                )
//
//                // 4) Кто и куда может ходить без авторизации
//                .authorizeHttpRequests(auth -> auth
//                        // публичные API (регистрация, получения токена, OAuth callbacks и документация)
//                        .requestMatchers(
//                                "/authentication/**",
//                                "/user/**",
//                                "/admin/**",
//                                "/v2/api-docs",
//                                "/v3/api-docs",
//                                "/v3/api-docs/**",
//                                "/swagger-resources",
//                                "/swagger-resources/**",
//                                "/configuration/ui",
//                                "/configuration/security",
//                                "/swagger-ui/**",
//                                "/webjars/**",
//                                "/swagger-ui.html",
//                                "/login/**",
//                                "/oauth2/**", // Важно для OAuth2
//                                "/login/oauth2/code/google"
//                        ).permitAll()
//
//                        // остальным — нужна авторизация
//                        .anyRequest().authenticated()
//                )
//
//                // 5) Наш JWT-фильтр
//                .authenticationProvider(authenticationProvider)
//                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
//
//                // 6) Если кто-то неавторизован — пусть возвращает 401, а не редиректит.
//                .exceptionHandling(ex -> ex
//                        .authenticationEntryPoint(
//                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
//                )
//        ;
//
//        return http.build();
//    }
//    /**
//     * Конфигурация фильтра CORS.
//     */
//    @Bean
//    public FilterRegistrationBean<CorsFilter> corsFilter() {
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        CorsConfiguration config = new CorsConfiguration();
//        config.setAllowCredentials(true);
//
//        // добавляем адреса, с которых будем принимать запросы
//        config.addAllowedOrigin("http://localhost:5173");    // ваш Vite-dev
//        config.addAllowedOrigin("https://www.gwork.press");
//        config.addAllowedOrigin("https://api.center.beer");  // если фронт на этом же хосте
//        config.addAllowedOrigin("https://center.beer");      // при необходимости
//
//        config.setAllowedHeaders(List.of(
//                HttpHeaders.AUTHORIZATION,
//                HttpHeaders.CONTENT_TYPE,
//                HttpHeaders.ACCEPT
//        ));
//        config.setAllowedMethods(List.of(
//                HttpMethod.GET.name(),
//                HttpMethod.POST.name(),
//                HttpMethod.PUT.name(),
//                HttpMethod.DELETE.name(),
//                HttpMethod.OPTIONS.name()
//        ));
//        config.setMaxAge(MAX_AGE);
//
//        source.registerCorsConfiguration("/**", config);
//
//        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
//        bean.setOrder(CORS_FILTER_ORDER);
//        return bean;
//    }
//
//}


//@Configuration
//@EnableWebSecurity
//@RequiredArgsConstructor
//public class SecurityConfiguration {
//
//    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
//    private final JwtAuthenticationFilter jwtAuthenticationFilter;
//    private final AuthenticationProvider authenticationProvider;
//
//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http,
//                                                   CorsConfigurationSource corsSource) throws Exception {
//
//        http
//                .cors(cors -> cors.configurationSource(corsSource))    // <–– подключаем CORS к Spring Security
//                .csrf(AbstractHttpConfigurer::disable)
//                .sessionManagement(m -> m.sessionCreationPolicy(STATELESS))
//
//                .formLogin(AbstractHttpConfigurer::disable)
//                .oauth2Login(o -> o
//                        .loginPage("/oauth2/authorization/vk")
//                        .successHandler(oAuth2LoginSuccessHandler)
//                )
//
//                .exceptionHandling(e -> e
//                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
//                )
//
//                .authorizeHttpRequests(a -> a
//                        .requestMatchers(
//                                "/authentication/**",
//                                "/oauth2/**",
//                                "/login/oauth2/code/**",
//                                "/v3/api-docs/**", "/swagger-ui/**"
//                        ).permitAll()
//                        .anyRequest().authenticated()
//                )
//
//                .authenticationProvider(authenticationProvider)
//                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
//
//        return http.build();
//    }
//
//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration cfg = new CorsConfiguration();
//        cfg.setAllowCredentials(true);
//        // явно укажите все origin-ы, с которых будете дергать ваш API:
//        cfg.setAllowedOriginPatterns(List.of(
//                "http://localhost:5173",
//                "https://new.center.beer",
//                "https://api.center.beer",
//                "https://www.gwork.press"
//        ));
//        cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
//        cfg.setAllowedHeaders(List.of("Authorization","Content-Type","Accept"));
//        // по надобности:
//        cfg.setExposedHeaders(List.of("Authorization"));
//
//        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
//        src.registerCorsConfiguration("/**", cfg);
//        return src;
//    }
//}

import fr.mossaab.security.service.OAuth2LoginSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationProvider authenticationProvider;

    /**
     * Основной бин CorsConfigurationSource, помечен @Primary,
     * чтобы никакие другие реализации не конфликтовали.
     */
    @Bean
    @Primary
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173",
                "https://www.gwork.press",
                "https://api.center.beer",
                "https://center.beer"
        ));
        config.setAllowedHeaders(Arrays.asList(
                HttpHeaders.AUTHORIZATION,
                HttpHeaders.CONTENT_TYPE,
                HttpHeaders.ACCEPT
        ));
        config.setAllowedMethods(Arrays.asList(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name()
        ));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * Основная цепочка безопасности.
     * При неавторизованном доступе возвращаем 401,
     * настраиваем stateless-сессию, OAuth2-login и JWT-фильтр.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   CorsConfigurationSource corsSource) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsSource))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/oauth2/authorization/vk")
                        .successHandler(oAuth2LoginSuccessHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/authentication/**",
                                "/user/**",
                                "/admin/**",
                                "/v2/api-docs",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/swagger-resources",
                                "/swagger-resources/**",
                                "/configuration/ui",
                                "/configuration/security",
                                "/swagger-ui/**",
                                "/webjars/**",
                                "/swagger-ui.html",
                                "/login/**",
                                "/oauth2/**", // Важно для OAuth2
                                "/login/oauth2/code/google"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                );

        return http.build();
    }
}
