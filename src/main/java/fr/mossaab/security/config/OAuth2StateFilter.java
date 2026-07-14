package fr.mossaab.security.config;

import fr.mossaab.security.service.social.hendler.OAuthStateStorage;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Фильтр для сохранения OAuth2 state параметра перед редиректом на провайдера.
 * В stateless-режиме Spring не может сохранять state в сессии, поэтому используем кастомное хранилище.
 */
@Component
@RequiredArgsConstructor
public class OAuth2StateFilter extends OncePerRequestFilter {

    private final OAuthStateStorage stateStorage;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // Проверяем, является ли запрос запросом на OAuth2 авторизацию
        String requestUri = request.getRequestURI();
        if (requestUri != null && requestUri.startsWith("/oauth2/authorization/")) {
            // Сохраняем state параметр, если он есть
            String state = request.getParameter("state");
            if (state != null && !state.isEmpty()) {
                // Сохраняем state в хранилище с фиксированным значением
                // Значение не важно, важно только то, что state был сгенерирован Spring
                stateStorage.put(state, "stored");
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
