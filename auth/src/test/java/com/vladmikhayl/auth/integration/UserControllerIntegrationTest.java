package com.vladmikhayl.auth.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladmikhayl.auth.dto.RegisterRequest;
import com.vladmikhayl.auth.entity.User;
import com.vladmikhayl.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test") // чтобы CommandLineRunner в коде SecurityConfig не выполнялся
@TestPropertySource(properties = {
        // чтобы Спринг не пытался использовать конфиг-сервер и Эврику
        "spring.config.location=classpath:/application-test.yml",
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false"
})
@Transactional // чтобы после каждого теста все изменения, сделанные в БД, откатывались обратно
@AutoConfigureMockMvc
public class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private static ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeAll
    public static void setUp() {
        objectMapper = new ObjectMapper();

        // Явным образом получаем контейнер Postgres (если он еще не создавался, то в этот момент создастся его синглтон)
        TestPostgresContainer.getInstance();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Задаем настройки для БД, используя синглтон контейнера Postgres
        TestPostgresContainer container = TestPostgresContainer.getInstance();
        registry.add("spring.datasource.url", container::getJdbcUrl);
        registry.add("spring.datasource.username", container::getUsername);
        registry.add("spring.datasource.password", container::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Test
    void canRegisterWithCorrectUsernameAndPassword() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("123")
                .password("12345")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        Optional<User> user = userRepository.findByUsername(request.getUsername());

        assertThat(user.isPresent()).isTrue();

        assertThat(user.get().getUsername()).isEqualTo("123");
        assertThat(user.get().getPassword()).isNotBlank();
    }

    @Test
    void failRegisterWhenThatUsernameIsTaken() throws Exception {
        User existing = User.builder()
                .username("123")
                .passwordHash("hashedPassword")
                .build();

        userRepository.save(existing);

        RegisterRequest request = RegisterRequest.builder()
                .username("123")
                .password("12345")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Username already exists"));

        long usersCount = userRepository.count();
        assertThat(usersCount).isEqualTo(1);
    }

    @Test
    void failRegisterWithBlankUsernameAndPassword() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("")
                .password("")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isNotEmpty());

        long usersCount = userRepository.count();
        assertThat(usersCount).isEqualTo(0);
    }

}
