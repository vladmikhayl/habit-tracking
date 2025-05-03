package com.vladmikhayl.auth.integration;

import com.vladmikhayl.auth.entity.User;
import com.vladmikhayl.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
public class InternalUserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeAll
    public static void setUp() {
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
    @Sql(statements = "ALTER SEQUENCE user_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void canGetUserLoginWhenUserExists() throws Exception {
        User existingUser = User.builder()
                .username("user1")
                .passwordHash("123")
                .build();

        userRepository.save(existingUser);

        mockMvc.perform(get("/internal/auth/1/get-login"))
                .andExpect(status().isOk())
                .andExpect(content().string("user1"));
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE user_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void failGetUserLoginWhenUserDoesNotExist() throws Exception {
        User existingUser = User.builder()
                .username("user1")
                .passwordHash("123")
                .build();

        userRepository.save(existingUser);

        mockMvc.perform(get("/internal/auth/2/get-login"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Пользователь не найден"));
    }

}
