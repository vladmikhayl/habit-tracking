package com.vladmikhayl.report.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vladmikhayl.report.dto.ReportCreationRequest;
import com.vladmikhayl.report.dto.ReportPhotoEditingRequest;
import com.vladmikhayl.report.entity.HabitPhotoAllowedCache;
import com.vladmikhayl.report.entity.Report;
import com.vladmikhayl.report.repository.HabitPhotoAllowedCacheRepository;
import com.vladmikhayl.report.repository.ReportRepository;
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
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
public class ReportControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private HabitPhotoAllowedCacheRepository habitPhotoAllowedCacheRepository;

    private static ObjectMapper objectMapper;

    @BeforeAll
    public static void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule());

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
    @Sql(statements = "ALTER SEQUENCE report_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void canCreateReportWithoutPhotoWhenPhotoIsNotAllowed() throws Exception {
        ReportCreationRequest request = ReportCreationRequest.builder()
                .habitId(1L)
                .date(LocalDate.of(2025, 3, 28))
                .photoUrl(null)
                .build();

        String userIdStr = "2";
        Long userId = 2L;

        mockMvc.perform(post("/api/v1/reports/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isCreated());

        Optional<Report> report = reportRepository.findByHabitIdAndDate(request.getHabitId(), request.getDate());
        assertThat(report.isPresent()).isTrue();

        Report expected = Report.builder()
                .id(1L)
                .userId(userId)
                .habitId(1L)
                .date(LocalDate.of(2025, 3, 28))
                .photoUrl(null)
                .build();

        assertThat(report.get())
                .usingRecursiveComparison()
                .ignoringFields("createdAt")
                .isEqualTo(expected);
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE report_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void canCreateReportWithoutPhotoWhenPhotoIsAllowed() throws Exception {
        Long habitId = 10L;

        ReportCreationRequest request = ReportCreationRequest.builder()
                .habitId(habitId)
                .date(LocalDate.of(2025, 3, 28))
                .photoUrl(null)
                .build();

        String userIdStr = "2";
        Long userId = 2L;

        habitPhotoAllowedCacheRepository.save(
                HabitPhotoAllowedCache.builder()
                        .habitId(habitId)
                        .build()
        );

        mockMvc.perform(post("/api/v1/reports/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isCreated());

        Optional<Report> report = reportRepository.findByHabitIdAndDate(request.getHabitId(), request.getDate());
        assertThat(report.isPresent()).isTrue();

        Report expected = Report.builder()
                .id(1L)
                .userId(userId)
                .habitId(habitId)
                .date(LocalDate.of(2025, 3, 28))
                .photoUrl(null)
                .build();

        assertThat(report.get())
                .usingRecursiveComparison()
                .ignoringFields("createdAt")
                .isEqualTo(expected);
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE report_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void canCreateReportWithPhotoWhenPhotoIsAllowed() throws Exception {
        Long habitId = 10L;

        ReportCreationRequest request = ReportCreationRequest.builder()
                .habitId(habitId)
                .date(LocalDate.of(2025, 3, 28))
                .photoUrl("https://start.spring.io/")
                .build();

        String userIdStr = "2";
        Long userId = 2L;

        habitPhotoAllowedCacheRepository.save(
                HabitPhotoAllowedCache.builder()
                        .habitId(habitId)
                        .build()
        );

        mockMvc.perform(post("/api/v1/reports/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isCreated());

        Optional<Report> report = reportRepository.findByHabitIdAndDate(request.getHabitId(), request.getDate());
        assertThat(report.isPresent()).isTrue();

        Report expected = Report.builder()
                .id(1L)
                .userId(userId)
                .habitId(habitId)
                .date(LocalDate.of(2025, 3, 28))
                .photoUrl("https://start.spring.io/")
                .build();

        assertThat(report.get())
                .usingRecursiveComparison()
                .ignoringFields("createdAt")
                .isEqualTo(expected);
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE report_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void failCreateReportWithPhotoWhenPhotoIsNotAllowed() throws Exception {
        Long habitId = 10L;

        ReportCreationRequest request = ReportCreationRequest.builder()
                .habitId(habitId)
                .date(LocalDate.of(2025, 3, 28))
                .photoUrl("https://start.spring.io/")
                .build();

        String userIdStr = "2";
        Long userId = 2L;

        mockMvc.perform(post("/api/v1/reports/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("This habit doesn't imply a photo, but it was attached"));

        long reportsCount = reportRepository.count();
        assertThat(reportsCount).isEqualTo(0);
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE report_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void failCreateReportWithFutureDate() throws Exception {
        Long habitId = 10L;

        ReportCreationRequest request = ReportCreationRequest.builder()
                .habitId(habitId)
                .date(LocalDate.now().plusDays(1))
                .photoUrl(null)
                .build();

        String userIdStr = "2";
        Long userId = 2L;

        mockMvc.perform(post("/api/v1/reports/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("It is forbidden to mark a habit as completed for a day that has not yet arrived"));

        long reportsCount = reportRepository.count();
        assertThat(reportsCount).isEqualTo(0);
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE report_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void failCreateReportThatHasAlreadyBeenCreated() throws Exception {
        Long habitId = 10L;

        String userIdStr = "2";
        Long userId = 2L;

        Report existingReport = Report.builder()
                .userId(userId)
                .habitId(habitId)
                .date(LocalDate.of(2025, 3, 20))
                .photoUrl(null)
                .build();

        reportRepository.save(existingReport);

        ReportCreationRequest request = ReportCreationRequest.builder()
                .habitId(habitId)
                .date(LocalDate.of(2025, 3, 20))
                .photoUrl(null)
                .build();

        mockMvc.perform(post("/api/v1/reports/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("This habit has already been marked as completed on this day"));

        long reportsCount = reportRepository.count();
        assertThat(reportsCount).isEqualTo(1);
    }

    // TODO: тест на createReport когда у юзера нет такой привычки в этот день

    @Test
    @Sql(statements = "ALTER SEQUENCE report_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void canChangeReportPhotoWhenItHasAlreadyBeenAttached() throws Exception {
        String userIdStr = "2";
        Long userId = 2L;
        Long habitId = 10L;

        habitPhotoAllowedCacheRepository.save(
                HabitPhotoAllowedCache.builder()
                        .habitId(habitId)
                        .build()
        );

        Report existingReport = Report.builder()
                .userId(userId)
                .habitId(habitId)
                .date(LocalDate.of(2025, 3, 28))
                .photoUrl("https://old-photo-url.com/")
                .build();

        reportRepository.save(existingReport);

        ReportPhotoEditingRequest request = ReportPhotoEditingRequest.builder()
                .photoUrl("https://new-photo-url.com/")
                .build();

        mockMvc.perform(put("/api/v1/reports/1/change-photo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isOk());

        Optional<Report> report = reportRepository.findById(1L);
        assertThat(report.isPresent()).isTrue();
        assertThat(report.get().getPhotoUrl()).isEqualTo("https://new-photo-url.com/");

        long reportsCount = reportRepository.count();
        assertThat(reportsCount).isEqualTo(1);
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE report_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void canChangeReportPhotoWhenItHasNotBeenAttachedYet() throws Exception {
        String userIdStr = "2";
        Long userId = 2L;
        Long habitId = 10L;

        habitPhotoAllowedCacheRepository.save(
                HabitPhotoAllowedCache.builder()
                        .habitId(habitId)
                        .build()
        );

        Report existingReport = Report.builder()
                .userId(userId)
                .habitId(habitId)
                .date(LocalDate.of(2025, 3, 28))
                .photoUrl(null)
                .build();

        reportRepository.save(existingReport);

        ReportPhotoEditingRequest request = ReportPhotoEditingRequest.builder()
                .photoUrl("https://new-photo-url.com/")
                .build();

        mockMvc.perform(put("/api/v1/reports/1/change-photo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isOk());

        Optional<Report> report = reportRepository.findById(1L);
        assertThat(report.isPresent()).isTrue();
        assertThat(report.get().getPhotoUrl()).isEqualTo("https://new-photo-url.com/");

        long reportsCount = reportRepository.count();
        assertThat(reportsCount).isEqualTo(1);
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE report_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void canChangeReportPhotoToNull() throws Exception {
        String userIdStr = "2";
        Long userId = 2L;
        Long habitId = 10L;

        habitPhotoAllowedCacheRepository.save(
                HabitPhotoAllowedCache.builder()
                        .habitId(habitId)
                        .build()
        );

        Report existingReport = Report.builder()
                .userId(userId)
                .habitId(habitId)
                .date(LocalDate.of(2025, 3, 28))
                .photoUrl("https://old-photo-url.com/")
                .build();

        reportRepository.save(existingReport);

        ReportPhotoEditingRequest request = ReportPhotoEditingRequest.builder()
                .photoUrl("")
                .build();

        mockMvc.perform(put("/api/v1/reports/1/change-photo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isOk());

        Optional<Report> report = reportRepository.findById(1L);
        assertThat(report.isPresent()).isTrue();
        assertThat(report.get().getPhotoUrl()).isNull();

        long reportsCount = reportRepository.count();
        assertThat(reportsCount).isEqualTo(1);
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE report_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void failChangeReportPhotoWhenUserDoesNotHaveThisReport() throws Exception {
        String userIdStr = "2";

        ReportPhotoEditingRequest request = ReportPhotoEditingRequest.builder()
                .photoUrl("https://new-photo-url.com/")
                .build();

        mockMvc.perform(put("/api/v1/reports/1/change-photo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("This user doesn't have this report"));

        long reportsCount = reportRepository.count();
        assertThat(reportsCount).isEqualTo(0);
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE report_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void failChangeReportPhotoWhenThisHabitDoesNotImplyPhotos() throws Exception {
        String userIdStr = "2";
        Long userId = 2L;
        Long habitId = 10L;

        Report existingReport = Report.builder()
                .userId(userId)
                .habitId(habitId)
                .date(LocalDate.of(2025, 3, 28))
                .photoUrl(null)
                .build();

        reportRepository.save(existingReport);

        ReportPhotoEditingRequest request = ReportPhotoEditingRequest.builder()
                .photoUrl("https://new-photo-url.com/")
                .build();

        mockMvc.perform(put("/api/v1/reports/1/change-photo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("This habit doesn't imply a photo"));

        Optional<Report> report = reportRepository.findById(1L);
        assertThat(report.isPresent()).isTrue();
        assertThat(report.get().getPhotoUrl()).isNull();

        long reportsCount = reportRepository.count();
        assertThat(reportsCount).isEqualTo(1);
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE report_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void canDeleteReport() throws Exception {
        String userIdStr = "2";
        Long userId = 2L;
        Long habitId = 10L;

        Report existingReport = Report.builder()
                .userId(userId)
                .habitId(habitId)
                .date(LocalDate.of(2025, 3, 28))
                .photoUrl(null)
                .build();

        reportRepository.save(existingReport);

        assertThat(reportRepository.count()).isEqualTo(1);

        mockMvc.perform(delete("/api/v1/reports/1/delete")
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isOk());

        assertThat(reportRepository.count()).isEqualTo(0);
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE report_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void failDeleteReportWhenThatReportDoesNotExist() throws Exception {
        String userIdStr = "2";

        mockMvc.perform(delete("/api/v1/reports/1/delete")
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("This user doesn't have this report"));

        assertThat(reportRepository.count()).isEqualTo(0);
    }

    @Test
    @Sql(statements = "ALTER SEQUENCE report_seq RESTART WITH 1", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void failDeleteReportWhenThatReportBelongsToAnotherUser() throws Exception {
        String userIdStr = "2";
        Long habitId = 10L;

        Report existingReport = Report.builder()
                .userId(1L)
                .habitId(habitId)
                .date(LocalDate.of(2025, 3, 28))
                .photoUrl(null)
                .build();

        reportRepository.save(existingReport);

        assertThat(reportRepository.count()).isEqualTo(1);

        mockMvc.perform(delete("/api/v1/reports/1/delete")
                        .header("X-User-Id", userIdStr))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("This user doesn't have this report"));

        assertThat(reportRepository.count()).isEqualTo(1);
    }

}
