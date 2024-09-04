package fr.mossaab.security.config;

import fr.mossaab.security.service.api.JwtService;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Класс JwtAuthenticationFilter расширяет OncePerRequestFilter для выполнения на каждом HTTP-запросе.
 * Мы также можем реализовать интерфейс Filter (Jakarta EE), но Spring предоставляет нам OncePerRequestFilter,
 * который расширяет GenericFilterBean, который также реализует интерфейс Filter.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService; // Сервис для работы с JWT
    private final UserDetailsService userDetailsService; // Реализация предоставляется в ApplicationSecurityConfig

    // Список URI, для которых не нужно выполнять фильтрацию
    private static final List<String> EXCLUDED_URLS = List.of(
            "/passport/**",
            "/heatingSystem/**",
            "/auth/**",
            "/user/**",
            "/admin/**",
            "/v2/api-docs",
            "/v3/api-docs",
            "/swagger-resources/**",
            "/swagger-ui/**",
            "/webjars/**",
            "/swagger-ui.html",
            "/types/**",
            "/kinds/**",
            "/documents/**",
            "/service-centres/**"
    );

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Проверка, если текущий запрос нужно исключить из JWT-фильтрации
        if (isExcludedUrl(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        // Попытка получить JWT из куки или из заголовка Authorization
        String jwt = jwtService.getJwtFromCookies(request);
        final String authHeader = request.getHeader("Authorization");

        // Если JWT отсутствует в куках и заголовке Authorization, или если запрос - это запрос на аутентификацию,
        // пропустить фильтрацию и перейти к следующему фильтру в цепочке
        if ((jwt == null && (authHeader == null || !authHeader.startsWith("Bearer "))) ||
                request.getRequestURI().contains("/auth")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Если JWT отсутствует в куках, но присутствует в заголовке Authorization
        if (jwt == null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7); // после "Bearer "
        }

        // Извлечение имени пользователя из JWT
        final String userEmail = jwtService.extractUserName(jwt);

        // Если имя пользователя не пустое и аутентификация еще не выполнена
        if (StringUtils.isNotEmpty(userEmail)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Загрузка информации о пользователе на основе имени пользователя из хранилища
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
            // Проверка валидности токена JWT для пользователя
            if (jwtService.isTokenValid(jwt, userDetails)) {
                // Обновление контекста безопасности Spring Security путем добавления нового UsernamePasswordAuthenticationToken
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                context.setAuthentication(authToken);
                SecurityContextHolder.setContext(context);
            }
        }
        // Пропустить запрос к следующему фильтру в цепочке
        filterChain.doFilter(request, response);
    }

    /**
     * Проверяет, нужно ли исключить URL из фильтрации.
     *
     * @param requestURI URI запроса
     * @return true, если URL исключен, иначе false
     */
    private boolean isExcludedUrl(String requestURI) {
        return EXCLUDED_URLS.stream().anyMatch(requestURI::startsWith);
    }
}