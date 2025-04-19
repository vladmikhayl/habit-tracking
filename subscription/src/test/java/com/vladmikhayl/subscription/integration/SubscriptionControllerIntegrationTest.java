package com.vladmikhayl.subscription.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladmikhayl.subscription.FeignClientTestConfig;
import com.vladmikhayl.subscription.dto.response.AcceptedSubscriptionForCreatorResponse;
import com.vladmikhayl.subscription.dto.response.UnprocessedRequestForCreatorResponse;
import com.vladmikhayl.subscription.dto.response.UnprocessedRequestForSubscriberResponse;
import com.vladmikhayl.subscription.entity.HabitCache;
import com.vladmikhayl.subscription.entity.Subscription;
import com.vladmikhayl.subscription.repository.HabitCacheRepository;
import com.vladmikhayl.subscription.repository.SubscriptionRepository;
import com.vladmikhayl.subscription.service.feign.AuthClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test") // чтобы CommandLineRunner в коде Application не выполнялся
@TestPropertySource(properties = {
        // чтобы Спринг не пытался использовать конфиг-сервер и Эврику
        "spring.config.location=classpath:/application-test.yml",
        "eureka.client.enabled=false",
        "spring.cloud.config.enabled=false"
})
@Transactional // чтобы после каждого теста все изменения, сделанные в БД, откатывались обратно
// чтобы создалась встроенная Кафка, которая не будет отправлять сообщения на реальные микросервисы
@EmbeddedKafka(partitions = 1, topics = {"accepted-subscription-created", "accepted-subscription-deleted"})
@Import(FeignClientTestConfig.class) // импортируем конфиг, где мы создали замоканный бин Feign-клиента
@AutoConfigureMockMvc
public class SubscriptionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private HabitCacheRepository habitCacheRepository;

    @Autowired
    private AuthClient authClient;

    @Autowired
    private static ObjectMapper objectMapper;

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

    @Test
    @Sql(statements = "ALTER SEQUENCE subscription_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void canAcceptSubscriptionRequestWhenThatRequestIsForHabitOfCurrentUser() throws Exception {
        Long habitId = 15L;
        Long currentUserId = 7L;
        String currentUserIdStr = "7";

        Subscription existingSubscription = Subscription.builder()
                .subscriberId(5L)
                .habitId(habitId)
                .isAccepted(false)
                .build();

        subscriptionRepository.save(existingSubscription);

        HabitCache existingHabitCache = HabitCache.builder()
                .habitId(habitId)
                .creatorId(currentUserId)
                .build();

        habitCacheRepository.save(existingHabitCache);

        Mockito.when(authClient.getUserLogin(currentUserId)).thenReturn(ResponseEntity.ok("user7"));

        mockMvc.perform(put("/api/v1/subscriptions/1/accept")
                        .header("X-User-Id", currentUserIdStr))
                .andExpect(status().isOk());

        assertThat(subscriptionRepository.count()).isEqualTo(1);

        Subscription subscription = subscriptionRepository.findById(1L).get();

        assertThat(subscription.getHabitId()).isEqualTo(habitId);
        assertThat(subscription.getSubscriberId()).isEqualTo(5L);
        assertThat(subscription.isAccepted()).isTrue();
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE subscription_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void failAcceptSubscriptionRequestWhenThatRequestIsAlreadyAccepted() throws Exception {
        Long habitId = 15L;
        Long currentUserId = 7L;
        String currentUserIdStr = "7";

        Subscription existingSubscription = Subscription.builder()
                .subscriberId(5L)
                .habitId(habitId)
                .isAccepted(false)
                .build();

        subscriptionRepository.save(existingSubscription);

        HabitCache existingHabitCache = HabitCache.builder()
                .habitId(habitId)
                .creatorId(currentUserId)
                .build();

        habitCacheRepository.save(existingHabitCache);

        Mockito.when(authClient.getUserLogin(currentUserId)).thenReturn(ResponseEntity.ok("user7"));

        mockMvc.perform(put("/api/v1/subscriptions/1/accept")
                        .header("X-User-Id", currentUserIdStr))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/subscriptions/1/accept")
                        .header("X-User-Id", currentUserIdStr))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Subscription request has already been accepted"));
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE subscription_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void failAcceptSubscriptionRequestWhenThatRequestIsNotForHabitOfCurrentUser() throws Exception {
        Long habitId = 15L;
        String currentUserIdStr = "7";

        Subscription existingSubscription = Subscription.builder()
                .subscriberId(5L)
                .habitId(habitId)
                .isAccepted(false)
                .build();

        subscriptionRepository.save(existingSubscription);

        HabitCache existingHabitCache = HabitCache.builder()
                .habitId(habitId)
                .creatorId(8L)
                .build();

        habitCacheRepository.save(existingHabitCache);

        mockMvc.perform(put("/api/v1/subscriptions/1/accept")
                        .header("X-User-Id", currentUserIdStr))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("The user is not the creator of the habit that the subscription request is for"));
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE subscription_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void canDenySubscriptionRequestWhenThatRequestIsForHabitOfCurrentUser() throws Exception {
        Long habitId = 15L;
        Long currentUserId = 7L;
        String currentUserIdStr = "7";

        Subscription existingSubscription = Subscription.builder()
                .subscriberId(5L)
                .habitId(habitId)
                .isAccepted(false)
                .build();

        subscriptionRepository.save(existingSubscription);

        HabitCache existingHabitCache = HabitCache.builder()
                .habitId(habitId)
                .creatorId(currentUserId)
                .build();

        habitCacheRepository.save(existingHabitCache);

        assertThat(subscriptionRepository.count()).isEqualTo(1);

        mockMvc.perform(delete("/api/v1/subscriptions/1/deny")
                        .header("X-User-Id", currentUserIdStr))
                .andExpect(status().isOk());

        assertThat(subscriptionRepository.count()).isEqualTo(0);
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE subscription_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void failDenySubscriptionRequestWhenThatRequestIsAlreadyAccepted() throws Exception {
        Long habitId = 15L;
        Long currentUserId = 7L;
        String currentUserIdStr = "7";

        Subscription existingSubscription = Subscription.builder()
                .subscriberId(5L)
                .habitId(habitId)
                .isAccepted(true)
                .build();

        subscriptionRepository.save(existingSubscription);

        HabitCache existingHabitCache = HabitCache.builder()
                .habitId(habitId)
                .creatorId(currentUserId)
                .build();

        habitCacheRepository.save(existingHabitCache);

        assertThat(subscriptionRepository.count()).isEqualTo(1);

        mockMvc.perform(delete("/api/v1/subscriptions/1/deny")
                        .header("X-User-Id", currentUserIdStr))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Subscription request is already accepted"));

        assertThat(subscriptionRepository.count()).isEqualTo(1);
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE subscription_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void failDenySubscriptionRequestWhenThatRequestIsNotForHabitOfCurrentUser() throws Exception {
        Long habitId = 15L;
        String currentUserIdStr = "7";

        Subscription existingSubscription = Subscription.builder()
                .subscriberId(5L)
                .habitId(habitId)
                .isAccepted(false)
                .build();

        subscriptionRepository.save(existingSubscription);

        HabitCache existingHabitCache = HabitCache.builder()
                .habitId(habitId)
                .creatorId(8L)
                .build();

        habitCacheRepository.save(existingHabitCache);

        assertThat(subscriptionRepository.count()).isEqualTo(1);

        mockMvc.perform(delete("/api/v1/subscriptions/1/deny")
                        .header("X-User-Id", currentUserIdStr))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("The user is not the creator of the habit that the subscription request is for"));

        assertThat(subscriptionRepository.count()).isEqualTo(1);
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE subscription_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void canUnsubscribeWithAcceptedSubscription() throws Exception {
        Long habitId = 15L;
        Long currentUserId = 7L;
        String currentUserIdStr = "7";

        Subscription existingSubscription = Subscription.builder()
                .subscriberId(currentUserId)
                .habitId(habitId)
                .isAccepted(true)
                .build();

        subscriptionRepository.save(existingSubscription);

        HabitCache existingHabitCache = HabitCache.builder()
                .habitId(habitId)
                .creatorId(10L)
                .build();

        habitCacheRepository.save(existingHabitCache);

        assertThat(subscriptionRepository.count()).isEqualTo(1);

        mockMvc.perform(delete("/api/v1/subscriptions/15/unsubscribe")
                        .header("X-User-Id", currentUserIdStr))
                .andExpect(status().isOk());

        assertThat(subscriptionRepository.count()).isEqualTo(0);
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE subscription_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void failUnsubscribeWhenThatSubscriptionBelongsToAnotherUser() throws Exception {
        Long habitId = 15L;
        Long currentUserId = 7L;
        String currentUserIdStr = "7";

        // Подписка другого юзера на нужную привычку
        Subscription existingSubscription1 = Subscription.builder()
                .subscriberId(8L)
                .habitId(habitId)
                .isAccepted(false)
                .build();
        subscriptionRepository.save(existingSubscription1);

        // Подписка текущего юзера на другую привычку
        Subscription existingSubscription2 = Subscription.builder()
                .subscriberId(currentUserId)
                .habitId(16L)
                .isAccepted(false)
                .build();
        subscriptionRepository.save(existingSubscription2);

        HabitCache existingHabitCache = HabitCache.builder()
                .habitId(habitId)
                .creatorId(10L)
                .build();

        habitCacheRepository.save(existingHabitCache);

        assertThat(subscriptionRepository.count()).isEqualTo(2);

        mockMvc.perform(delete("/api/v1/subscriptions/15/unsubscribe")
                        .header("X-User-Id", currentUserIdStr))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Subscription (or subscription request) not found"));

        assertThat(subscriptionRepository.count()).isEqualTo(2);
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE subscription_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void testGetUserUnprocessedRequestsWithSomeUnprocessedRequests() throws Exception {
        Long currentUserId = 7L;
        String currentUserIdStr = "7";

        // Привычка 10
        HabitCache habitCache1 = HabitCache.builder()
                .habitId(10L)
                .habitName("Название 10")
                .build();
        habitCacheRepository.save(habitCache1);

        // Привычка 11
        HabitCache habitCache2 = HabitCache.builder()
                .habitId(11L)
                .habitName("Название 11")
                .build();
        habitCacheRepository.save(habitCache2);

        // Привычка 12
        HabitCache habitCache3 = HabitCache.builder()
                .habitId(12L)
                .habitName("Название 12")
                .build();
        habitCacheRepository.save(habitCache3);

        // Необработанная заявка от текущего юзера
        Subscription subscription1 = Subscription.builder()
                .subscriberId(currentUserId)
                .habitId(10L)
                .isAccepted(false)
                .build();
        subscriptionRepository.save(subscription1);

        // Необработанная заявка от текущего юзера
        Subscription subscription2 = Subscription.builder()
                .subscriberId(currentUserId)
                .habitId(11L)
                .isAccepted(false)
                .build();
        subscriptionRepository.save(subscription2);

        // Обработанная заявка от текущего юзера
        Subscription subscription3 = Subscription.builder()
                .subscriberId(currentUserId)
                .habitId(12L)
                .isAccepted(true)
                .build();
        subscriptionRepository.save(subscription3);

        // Необработанная заявка от другого юзера
        Subscription subscription4 = Subscription.builder()
                .subscriberId(8L)
                .habitId(12L)
                .isAccepted(false)
                .build();
        subscriptionRepository.save(subscription4);

        String expectedJson = objectMapper.writeValueAsString(
                List.of(
                        UnprocessedRequestForSubscriberResponse.builder()
                                .habitId(10L)
                                .habitName("Название 10")
                                .build(),
                        UnprocessedRequestForSubscriberResponse.builder()
                                .habitId(11L)
                                .habitName("Название 11")
                                .build()
                )
        );

        mockMvc.perform(get("/api/v1/subscriptions/get-user-unprocessed-requests")
                        .header("X-User-Id", currentUserIdStr))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedJson));
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE subscription_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void testGetUserUnprocessedRequestsWithoutUnprocessedRequests() throws Exception {
        String currentUserIdStr = "7";

        String expectedJson = objectMapper.writeValueAsString(
                List.of()
        );

        mockMvc.perform(get("/api/v1/subscriptions/get-user-unprocessed-requests")
                        .header("X-User-Id", currentUserIdStr))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedJson));
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE subscription_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void failGetHabitUnprocessedRequestsWhenHabitBelongsToAnotherUser() throws Exception {
        String currentUserIdStr = "7";
        Long habitId = 15L;

        // Привычка другого юзера
        HabitCache habitCache = HabitCache.builder()
                .habitId(habitId)
                .creatorId(8L)
                .build();
        habitCacheRepository.save(habitCache);

        mockMvc.perform(get("/api/v1/subscriptions/15/get-habit-unprocessed-requests")
                        .header("X-User-Id", currentUserIdStr))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("The habit doesn't belong to that user"));
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE subscription_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void canGetHabitUnprocessedRequestsWithZeroRequests() throws Exception {
        String currentUserIdStr = "7";
        Long currentUserId = 7L;
        Long habitId = 15L;

        // Искомая привычка текущего юзера
        HabitCache habitCache1 = HabitCache.builder()
                .habitId(habitId)
                .creatorId(currentUserId)
                .build();
        habitCacheRepository.save(habitCache1);

        // Другая привычка текущего юзера
        HabitCache habitCache2 = HabitCache.builder()
                .habitId(16L)
                .creatorId(currentUserId)
                .build();
        habitCacheRepository.save(habitCache2);

        // Подписка на другую привычку
        Subscription subscription = Subscription.builder()
                .habitId(16L)
                .subscriberId(10L)
                .isAccepted(false)
                .build();
        subscriptionRepository.save(subscription);

        String expectedJson = objectMapper.writeValueAsString(
                List.of()
        );

        mockMvc.perform(get("/api/v1/subscriptions/15/get-habit-unprocessed-requests")
                        .header("X-User-Id", currentUserIdStr))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedJson));
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE subscription_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void canGetHabitUnprocessedRequestsWithSomeRequests() throws Exception {
        String currentUserIdStr = "7";
        Long currentUserId = 7L;
        Long habitId = 15L;

        // Искомая привычка текущего юзера
        HabitCache habitCache1 = HabitCache.builder()
                .habitId(habitId)
                .creatorId(currentUserId)
                .build();
        habitCacheRepository.save(habitCache1);

        // Принятая подписка на нужную привычку
        Subscription subscription1 = Subscription.builder()
                .habitId(habitId)
                .subscriberId(10L)
                .isAccepted(true)
                .build();
        subscriptionRepository.save(subscription1);

        // Необработанная подписка на нужную привычку
        Subscription subscription2 = Subscription.builder()
                .habitId(habitId)
                .subscriberId(11L)
                .isAccepted(false)
                .build();
        subscriptionRepository.save(subscription2);

        // Необработанная подписка на нужную привычку
        Subscription subscription3 = Subscription.builder()
                .habitId(habitId)
                .subscriberId(12L)
                .isAccepted(false)
                .build();
        subscriptionRepository.save(subscription3);

        Mockito.when(authClient.getUserLogin(11L)).thenReturn(ResponseEntity.ok("user11"));
        Mockito.when(authClient.getUserLogin(12L)).thenReturn(ResponseEntity.ok("user12"));

        String expectedJson = objectMapper.writeValueAsString(
                List.of(
                        UnprocessedRequestForCreatorResponse.builder()
                                .subscriptionId(2L)
                                .subscriberLogin("user11")
                                .build(),
                        UnprocessedRequestForCreatorResponse.builder()
                                .subscriptionId(3L)
                                .subscriberLogin("user12")
                                .build()
                )
        );

        mockMvc.perform(get("/api/v1/subscriptions/15/get-habit-unprocessed-requests")
                        .header("X-User-Id", currentUserIdStr))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedJson));
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE subscription_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void failGetHabitAcceptedSubscriptionsWhenHabitBelongsToAnotherUser() throws Exception {
        String currentUserIdStr = "7";
        Long habitId = 15L;

        // Привычка другого юзера
        HabitCache habitCache = HabitCache.builder()
                .habitId(habitId)
                .creatorId(8L)
                .build();
        habitCacheRepository.save(habitCache);

        mockMvc.perform(get("/api/v1/subscriptions/15/get-habit-accepted-subscriptions")
                        .header("X-User-Id", currentUserIdStr))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("The habit doesn't belong to that user"));
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE subscription_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void canGetHabitAcceptedSubscriptionsWithZeroRequests() throws Exception {
        String currentUserIdStr = "7";
        Long currentUserId = 7L;
        Long habitId = 15L;

        // Искомая привычка текущего юзера
        HabitCache habitCache1 = HabitCache.builder()
                .habitId(habitId)
                .creatorId(currentUserId)
                .build();
        habitCacheRepository.save(habitCache1);

        // Другая привычка текущего юзера
        HabitCache habitCache2 = HabitCache.builder()
                .habitId(16L)
                .creatorId(currentUserId)
                .build();
        habitCacheRepository.save(habitCache2);

        // Подписка на другую привычку
        Subscription subscription = Subscription.builder()
                .habitId(16L)
                .subscriberId(10L)
                .isAccepted(true)
                .build();
        subscriptionRepository.save(subscription);

        String expectedJson = objectMapper.writeValueAsString(
                List.of()
        );

        mockMvc.perform(get("/api/v1/subscriptions/15/get-habit-accepted-subscriptions")
                        .header("X-User-Id", currentUserIdStr))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedJson));
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE subscription_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void canGetHabitAcceptedSubscriptionsWithSomeRequests() throws Exception {
        String currentUserIdStr = "7";
        Long currentUserId = 7L;
        Long habitId = 15L;

        // Искомая привычка текущего юзера
        HabitCache habitCache1 = HabitCache.builder()
                .habitId(habitId)
                .creatorId(currentUserId)
                .build();
        habitCacheRepository.save(habitCache1);

        // Принятая подписка на нужную привычку
        Subscription subscription1 = Subscription.builder()
                .habitId(habitId)
                .subscriberId(10L)
                .isAccepted(true)
                .build();
        subscriptionRepository.save(subscription1);

        // Принятая подписка на нужную привычку
        Subscription subscription2 = Subscription.builder()
                .habitId(habitId)
                .subscriberId(11L)
                .isAccepted(true)
                .build();
        subscriptionRepository.save(subscription2);

        // Необработанная подписка на нужную привычку
        Subscription subscription3 = Subscription.builder()
                .habitId(habitId)
                .subscriberId(12L)
                .isAccepted(false)
                .build();
        subscriptionRepository.save(subscription3);

        Mockito.when(authClient.getUserLogin(10L)).thenReturn(ResponseEntity.ok("user10"));
        Mockito.when(authClient.getUserLogin(11L)).thenReturn(ResponseEntity.ok("user11"));

        String expectedJson = objectMapper.writeValueAsString(
                List.of(
                        AcceptedSubscriptionForCreatorResponse.builder()
                                .subscriberLogin("user10")
                                .build(),
                        AcceptedSubscriptionForCreatorResponse.builder()
                                .subscriberLogin("user11")
                                .build()
                )
        );

        mockMvc.perform(get("/api/v1/subscriptions/15/get-habit-accepted-subscriptions")
                        .header("X-User-Id", currentUserIdStr))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedJson));
    }

}
