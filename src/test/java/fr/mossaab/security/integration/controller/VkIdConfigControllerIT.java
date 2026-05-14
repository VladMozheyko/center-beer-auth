package fr.mossaab.security.integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.mossaab.security.controller.VkIdConfigController.VkIdConfigResponse;
import fr.mossaab.security.logger.AuditLogger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Интеграционные тесты для VkIdConfigController.
 *
 * Поднимается почти полный контекст Spring Boot, дергается реальный контроллер,
 * значения @Value берутся из src/test/resources/application.yml.
 */
@SpringBootTest
@AutoConfigureMockMvc
class VkIdConfigControllerIT extends AbstractIntegrationTest{

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuditLogger auditLogger;

    @Test
    @DisplayName("GET /oauth2/vk_id-config возвращает корректную конфигурацию")
    void getVkIdConfig_returnsExpectedConfig() throws Exception {
        String responseJson = mockMvc.perform(get("/oauth2/vk_id-config")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.clientId").exists())
                .andExpect(jsonPath("$.scope").exists())
                .andExpect(jsonPath("$.redirectUri").exists())
                .andExpect(jsonPath("$.authBackendUrl").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        VkIdConfigResponse response = objectMapper.readValue(responseJson, VkIdConfigResponse.class);

        assertThat(response.getClientId()).isEqualTo("54378011");

        assertThat(response.getScope()).isEqualTo("openid profile email");

        assertThat(response.getRedirectUri()).isEqualTo("http://localhost:8080/login/oauth2/code/vk");
        assertThat(response.getAuthBackendUrl()).isEqualTo("http://localhost:8080");

        verifyNoMoreInteractions(auditLogger);
    }

    @Test
    @DisplayName("GET /oauth2/vk_id-config можно вызывать многократно, ответ стабилен")
    void getVkIdConfig_isIdempotent() throws Exception {
        String first = mockMvc.perform(get("/oauth2/vk_id-config")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String second = mockMvc.perform(get("/oauth2/vk_id-config")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        VkIdConfigResponse resp1 = objectMapper.readValue(first, VkIdConfigResponse.class);
        VkIdConfigResponse resp2 = objectMapper.readValue(second, VkIdConfigResponse.class);

        assertThat(resp1).usingRecursiveComparison().isEqualTo(resp2);
    }
}
