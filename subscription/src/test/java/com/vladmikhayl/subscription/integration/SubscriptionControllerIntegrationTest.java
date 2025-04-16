package com.vladmikhayl.subscription.integration;

import com.vladmikhayl.subscription.entity.HabitCache;
import com.vladmikhayl.subscription.entity.Subscription;
import com.vladmikhayl.subscription.repository.HabitCacheRepository;
import com.vladmikhayl.subscription.repository.SubscriptionRepository;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test") // чтобы CommandLineRunner в коде Application не выполнялся
@TestPropertySource(properties = {
        // чтобы Спринг не пытался использовать конфиг-сервер и Эврику
        "spring.config.location=classpath:/application-test.yml",
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false"
})
@Transactional // чтобы после каждого теста все изменения, сделанные в БД, откатывались обратно
@AutoConfigureMockMvc
public class SubscriptionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private HabitCacheRepository habitCacheRepository;

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
    @Sql(statements = "ALTER SEQUENCE subscription_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void canSendSubscriptionRequestWhenThatHabitBelongsToAnotherUser() throws Exception {
        Long habitId = 15L;
        Long subscriberId = 7L;
        String subscriberIdStr = "7";

        // Существует привычка 15 (создана другим юзером)
        HabitCache habitCache = HabitCache.builder()
                .creatorId(10L)
                .habitId(habitId)
                .build();
        habitCacheRepository.save(habitCache);

        // Какой-то другой юзер подписан на привычку 15
        Subscription existingSubscription1 = Subscription.builder()
                .subscriberId(20L)
                .habitId(habitId)
                .isAccepted(false)
                .build();
        subscriptionRepository.save(existingSubscription1);

        // Текущий юзер подписан на какую-то другую привычку
        Subscription existingSubscription2 = Subscription.builder()
                .subscriberId(subscriberId)
                .habitId(14L)
                .isAccepted(true)
                .build();
        subscriptionRepository.save(existingSubscription2);

        assertThat(subscriptionRepository.count()).isEqualTo(2);

        mockMvc.perform(post("/api/v1/subscriptions/15/send-subscription-request")
                        .header("X-User-Id", subscriberIdStr))
                .andExpect(status().isCreated());

        Optional<Subscription> subscription = subscriptionRepository.findById(3L);
        assertThat(subscription).isNotEmpty();
        assertThat(subscription.get().getHabitId()).isEqualTo(15L);
        assertThat(subscription.get().getSubscriberId()).isEqualTo(7L);
        assertThat(subscription.get().isAccepted()).isEqualTo(false);
        assertThat(subscriptionRepository.count()).isEqualTo(3);
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE subscription_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void failSendSubscriptionRequestWhenThatSubscriptionAlreadyExists() throws Exception {
        Long habitId = 15L;
        Long subscriberId = 7L;
        String subscriberIdStr = "7";

        // Существует привычка 15 (создана другим юзером)
        HabitCache habitCache = HabitCache.builder()
                .creatorId(10L)
                .habitId(habitId)
                .build();
        habitCacheRepository.save(habitCache);

        // Текущий юзер имеет заявку на подписку на привычку 15
        Subscription existingSubscription = Subscription.builder()
                .subscriberId(subscriberId)
                .habitId(habitId)
                .isAccepted(false)
                .build();
        subscriptionRepository.save(existingSubscription);

        assertThat(subscriptionRepository.count()).isEqualTo(1);

        mockMvc.perform(post("/api/v1/subscriptions/15/send-subscription-request")
                        .header("X-User-Id", subscriberIdStr))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("This user is already subscribed to this habit or has an unprocessed request for it"));

        assertThat(subscriptionRepository.count()).isEqualTo(1);
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE subscription_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void failSendSubscriptionRequestWhenThatHabitDoesNotExist() throws Exception {
        String subscriberIdStr = "7";

        assertThat(subscriptionRepository.count()).isEqualTo(0);

        mockMvc.perform(post("/api/v1/subscriptions/15/send-subscription-request")
                        .header("X-User-Id", subscriberIdStr))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Habit not found"));

        assertThat(subscriptionRepository.count()).isEqualTo(0);
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE subscription_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void failSendSubscriptionRequestWhenThatHabitBelongsToSubscriber() throws Exception {
        Long habitId = 15L;
        Long subscriberId = 7L;
        String subscriberIdStr = "7";

        // Существует привычка 15 (создана текущим юзером)
        HabitCache habitCache = HabitCache.builder()
                .creatorId(subscriberId)
                .habitId(habitId)
                .build();
        habitCacheRepository.save(habitCache);

        assertThat(subscriptionRepository.count()).isEqualTo(0);

        mockMvc.perform(post("/api/v1/subscriptions/15/send-subscription-request")
                        .header("X-User-Id", subscriberIdStr))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("It is impossible to subscribe to your own habit"));

        assertThat(subscriptionRepository.count()).isEqualTo(0);
    }

}
