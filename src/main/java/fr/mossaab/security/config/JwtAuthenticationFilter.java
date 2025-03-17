package fr.mossaab.security.config;

import fr.mossaab.security.service.JwtService;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService; // Сервис для работы с JWT
    private final UserDetailsService userDetailsService; // Сервис для загрузки пользователей

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // URL-ы, которые не должны проверяться фильтром JWT
        Set<String> urlsToSkip = Set.of(
                "/v2/api-docs",
                "/v3/api-docs",
                "/swagger-resources/",
                "/swagger-ui/",
                "/webjars/",
                "/swagger-ui.html",
                "/authentication/"
                ,"/quiz/"
        );

        String requestURI = request.getRequestURI();
        logger.debug("Processing request URI: {}", requestURI);

        // Пропуск URL-ов, которые не нужно проверять
        if (urlsToSkip.stream().anyMatch(requestURI::startsWith)) {
            logger.debug("Skipping authentication for URL: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        // Попытка получить JWT из куки или заголовка Authorization
        String jwt = jwtService.getJwtFromCookies(request);
        final String authHeader = request.getHeader("Authorization");

        if (jwt == null && authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7); // Извлечение токена после "Bearer "
            logger.debug("JWT extracted from Authorization header: {}", jwt);
        }

        if (jwt == null) {
            logger.debug("No JWT found in request. Proceeding without authentication.");
            filterChain.doFilter(request, response);
            return;
        }

        // Извлечение имени пользователя и роли из JWT
        String userEmail = jwtService.extractUserName(jwt);
        String role = jwtService.extractRole(jwt); // Извлечение роли из токена

        if (StringUtils.isNotEmpty(userEmail)) {
            logger.debug("Extracted userEmail: {} and role: {}", userEmail, role);
        } else {
            logger.warn("Failed to extract userEmail or role from JWT. Proceeding without authentication.");
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
            logger.debug("Loaded userDetails for userEmail: {}", userEmail);

            if (jwtService.isTokenValid(jwt, userDetails)) {
                // Добавление роли из токена в Authentication
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        List.of(new SimpleGrantedAuthority(role)) // Добавление роли
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
