package fr.mossaab.security.integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.mossaab.security.dto.social.SocialExchangeRequest;
import fr.mossaab.security.dto.social.SocialUserInfo;
import fr.mossaab.security.entities.RefreshToken;
import fr.mossaab.security.entities.User;
import fr.mossaab.security.entities.UserSocialAccount;
import fr.mossaab.security.enums.OAuthProvider;
import fr.mossaab.security.enums.Role;
import fr.mossaab.security.repository.UserRepository;
import fr.mossaab.security.repository.UserSocialAccountRepository;
import fr.mossaab.security.service.JwtService;
import fr.mossaab.security.service.RefreshTokenService;
import fr.mossaab.security.service.social.service.OneTimeAuthCodeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class OAuth2ControllerIT extends AbstractIntegrationTest{

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OneTimeAuthCodeService oneTimeAuthCodeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSocialAccountRepository socialAccountRepository;

    @SpyBean
    private JwtService jwtService;

    @SpyBean
    private RefreshTokenService refreshTokenService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("OAuth2 Регистрация через соцсеть")
    void socialRegister_ShouldRegisterNewUserWithSocialOAuthCode() throws Exception {
        SocialUserInfo userInfo = new SocialUserInfo();
        userInfo.setEmail("unique-social-user@example.com");
        userInfo.setId("newSocialId");

        String authCode = oneTimeAuthCodeService.issueCode(userInfo);

        assertThat(userRepository.findByEmail(userInfo.getEmail())).isEmpty();

        SocialExchangeRequest request = new SocialExchangeRequest();
        request.setAuthCode(authCode);
        request.setProvider(OAuthProvider.YANDEX);

        mockMvc.perform(post("/oauth2/social/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Регистрация успешна"));

        // Проверяем, что пользователь был добавлен в базу данных
        Optional<User> registeredUser = userRepository.findByEmail(userInfo.getEmail());
        assertThat(registeredUser).isPresent();
        assertThat(registeredUser.get().getEmail()).isEqualTo(userInfo.getEmail());
    }

    @Test
    @DisplayName("OAuth2 Вход через соцсеть")
    void socialLogin_ShouldAuthenticateUserWithSocialOAuthCode() throws Exception {
        SocialUserInfo userInfo = new SocialUserInfo();
        userInfo.setEmail("login-user@example.com");
        userInfo.setId("123456789");

        User existingUser = new User();
        existingUser.setEmail(userInfo.getEmail());
        existingUser.setNickname(userInfo.getEmail());
        existingUser.setSocialAccounts(new HashSet<>());
        existingUser.setRole(Role.USER);
        existingUser.setCreatedAt(LocalDateTime.now());
        userRepository.save(existingUser);

        UserSocialAccount usa = UserSocialAccount.builder()
                .socialEmail(userInfo.getEmail())
                .provider(OAuthProvider.GOOGLE)
                .externalId(userInfo.getId())
                .user(existingUser)
                .build();
        socialAccountRepository.save(usa);

        String authCode = oneTimeAuthCodeService.issueCode(userInfo);

        String jwtToken = "mocked-jwt-token";
        when(jwtService.generateToken(existingUser)).thenReturn(jwtToken);

        String refreshToken = "mocked-refresh-token";
        RefreshToken refreshTokenObject = new RefreshToken();
        refreshTokenObject.setToken(refreshToken);

        when(refreshTokenService.createRefreshToken(existingUser.getId())).thenReturn(refreshTokenObject);
        when(jwtService.generateJwtCookie(jwtToken)).thenReturn(ResponseCookie.from("jwt-token", jwtToken).build());
        when(refreshTokenService.generateRefreshTokenCookie(refreshToken)).thenReturn(ResponseCookie.from("refresh-token", refreshToken).build());

        SocialExchangeRequest requestSoc = new SocialExchangeRequest();
        requestSoc.setAuthCode(authCode);
        requestSoc.setProvider(OAuthProvider.GOOGLE);

        mockMvc.perform(post("/oauth2/social/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestSoc)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Успешный вход"))
                .andExpect(jsonPath("$.accessToken").value(jwtToken))
                .andExpect(jsonPath("$.refreshToken").value(refreshToken));
    }

    @Test
    @DisplayName("OAuth2 Привязка соцсети к существующему аккаунту")
    void socialLink_ShouldLinkSocialAccountToCurrentUser() throws Exception {
        User existingUser = new User();
        existingUser.setEmail("current-user@example.com");
        existingUser.setNickname("current-user");
        existingUser.setRole(Role.USER);
        existingUser.setCreatedAt(LocalDateTime.now());
        existingUser.setSocialAccounts(new HashSet<>());
        userRepository.save(existingUser);

        SocialUserInfo userInfo = new SocialUserInfo();
        userInfo.setEmail("current-user@example.com");
        userInfo.setId("linkSocialId");

        String authCode = oneTimeAuthCodeService.issueCode(userInfo);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("mocked-refresh-token");

        doReturn(refreshToken).when(refreshTokenService).createRefreshToken(any(Long.class));

        String jwtToken = "mocked-jwt-token";
        when(jwtService.generateToken(existingUser)).thenReturn(jwtToken);

        when(jwtService.generateJwtCookie(jwtToken)).thenReturn(ResponseCookie.from("jwt-token", jwtToken).build());
        when(refreshTokenService.generateRefreshTokenCookie("mocked-refresh-token")).thenReturn(ResponseCookie.from("refresh-token", "mocked-refresh-token").build());

        SocialExchangeRequest requestSoc = new SocialExchangeRequest();
        requestSoc.setAuthCode(authCode);
        requestSoc.setProvider(OAuthProvider.GOOGLE);

        mockMvc.perform(post("/oauth2/social/link")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(user(existingUser.getEmail()).roles("USER"))
                        .content(objectMapper.writeValueAsString(requestSoc)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Соцсеть привязана"));

        Optional<User> linkedUser = userRepository.findByEmail(userInfo.getEmail());
        assertThat(linkedUser).isPresent();
        assertThat(linkedUser.get().getEmail()).isEqualTo("current-user@example.com");
        assertThat(linkedUser.get().getSocialAccounts()).isNotEmpty();
    }
}