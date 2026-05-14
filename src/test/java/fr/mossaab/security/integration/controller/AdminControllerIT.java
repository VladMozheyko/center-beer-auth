package fr.mossaab.security.integration.controller;

import fr.mossaab.security.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Интеграционные тесты для AdminController")
class AdminControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Должен возвращать пользователей с пагинацией")
    void getAllUsers_ShouldReturnPaginatedUsers() throws Exception {
        mockMvc.perform(get("/admin/all-users")
                        .with(user("admin").roles("ADMIN"))
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.notify", is("Пользователи получены")))
                .andExpect(jsonPath("$.users", hasSize(lessThanOrEqualTo(10))))
                .andExpect(jsonPath("$.pageNumber", is(0)))
                .andExpect(jsonPath("$.pageSize", is(10)));
    }

    @Test
    @DisplayName("Должен возвращать ошибку 400, если страница недопустима")
    void getAllUsers_WithInvalidPage_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/admin/all-users")
                        .with(user("admin").roles("ADMIN"))
                        .param("page", "-1")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Должен возвращать ошибку 400, если размер страницы равен нулю")
    void getAllUsers_WithZeroPageSize_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/admin/all-users")
                        .with(user("admin").roles("ADMIN"))
                        .param("page", "0")
                        .param("size", "0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Должен возвращать ошибку 400, если размер страницы превышает допустимое значение")
    void getAllUsers_WithExcessivePageSize_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/admin/all-users")
                        .with(user("admin").roles("ADMIN"))
                        .param("page", "0")
                        .param("size", "101")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}