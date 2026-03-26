package fr.mossaab.security.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.entities.UserSocialAccount;
import fr.mossaab.security.enums.OAuthProvider;
import fr.mossaab.security.repository.UserRepository;
import fr.mossaab.security.service.OAuthCustomService;
import fr.mossaab.security.service.TokenResponseService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/oauth2")
@RequiredArgsConstructor
public class VkIdAuthController {

    private final TokenResponseService tokenResponseService;
    private final RestTemplate restTemplate;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final OAuthCustomService oAuthCustomService;
    private final UserRepository userRepository;

    @Value("${vk.oauth2.client-id}")
    private String clientId;

    @PostMapping("/vk/register")
    public void vkidLogin(@RequestBody Map<String, String> body,
                          HttpServletResponse response) throws IOException {

        String accessToken = body.get("access_token");
        if (accessToken == null || accessToken.isBlank()) {
            sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST, "access_token_missing", "VK access_token is missing");
            return;
        }

        Map<String, Object> userInfo = getVkUserInfo(accessToken);
        if (userInfo == null) {
            sendJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "user_info_failed", "Failed to fetch user info from VK ID");
            return;
        }

        String email = (String) userInfo.get("email");
        String firstName = (String) userInfo.get("first_name");
        String lastName = (String) userInfo.get("last_name");
        String userId = userInfo.get("user_id").toString();

        User user = oAuthCustomService.findOrCreateOAuthUser(userId, OAuthProvider.VK, email, firstName, lastName);
        tokenResponseService.sendTokenResponse(user, response);

    }

    @PostMapping("/vk/add_account")
    public void vkIDAddAccount(@RequestBody Map<String, String> body,
                               @AuthenticationPrincipal UserDetails currentUser,
                               HttpServletResponse response) throws IOException {

        // --- 1. Проверяем, что пользователь аутентифицирован ---
        if (currentUser == null) {
            sendJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "unauthorized", "User must be authenticated to link an account");
            return;
        }

        String accessToken = body.get("access_token");
        if (accessToken == null || accessToken.isBlank()) {
            sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST, "access_token_missing", "VK access_token is missing");
            return;
        }

        // --- 2. Получаем данные от VK ID ---
        Map<String, Object> userInfo = getVkUserInfo(accessToken);
        if (userInfo == null) {
            sendJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "user_info_failed", "Failed to fetch user info from VK ID");
            return;
        }

        String vkEmail = (String) userInfo.get("email");
        String userId = userInfo.get("user_id").toString();

        if (userId == null) {
            sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST, "invalid_user_id", "VK user_id is missing");
            return;
        }

        // --- 3. Получаем текущего пользователя из Spring Security ---
        String email = currentUser.getUsername();

        User loadedUser = userRepository.findByEmail(email).orElse(null);
        if (loadedUser == null) {
            sendJsonError(response, HttpServletResponse.SC_NOT_FOUND, "not_found", "No such a current logged in users");
            return;
        }

        Optional<UserSocialAccount> accounts = loadedUser.getSocialAccounts().stream()
                .filter(account -> account.getProvider().equals(OAuthProvider.VK)).findFirst();
        if (accounts.isPresent()) {
            sendJsonError(response, HttpStatus.CONFLICT.value(), "already_exist", "Already exist this social provider (VK) for the same Email.");
        }

        // --- 4. Добавляем аккаунт через сервис ---
        try {
            loadedUser = oAuthCustomService.addAccount(loadedUser, userId, OAuthProvider.VK, vkEmail);
        } catch (Exception e) {
            sendJsonError(response, HttpServletResponse.SC_CONFLICT, "link_failed", e.getMessage());
            return;
        }

        // --- 5. Отправляем успешный ответ ---
        Map<String, Object> responseBody = Map.of(
                "userName", loadedUser.getUsername(),
                "social_id", userId,
                "social_provider", "VK",
                "social_email", vkEmail,
                "status", "account_linked"
        );

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        new ObjectMapper().writeValue(response.getWriter(), responseBody);
    }

    /**
     * Запрашивает email и основные данные через VK ID OpenID Connect
     */
    private Map<String, Object> getVkUserInfo(String accessToken) {
        String url = "https://id.vk.ru/oauth2/user_info";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
        params.add("access_token", accessToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> body = response.getBody();

            if (body != null && body.containsKey("user")) {
                return (Map<String, Object>) body.get("user");
            } else {
                System.err.println("VKID /user_info вернул пустой ответ: " + body);
                return null;
            }
        } catch (RestClientException e) {
            System.err.println("Ошибка при вызове VKID /user_info: " + e.getMessage());
            return null;
        }
    }

    /**
     * Унифицированный JSON-ответ об ошибке
     */
    private void sendJsonError(HttpServletResponse response, int status, String error, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), Map.of(
                "error", error,
                "message", message
        ));
    }
}