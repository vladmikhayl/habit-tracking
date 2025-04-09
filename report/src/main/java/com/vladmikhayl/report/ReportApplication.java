package com.vladmikhayl.report;

import com.vladmikhayl.report.entity.HabitPhotoAllowedCache;
import com.vladmikhayl.report.repository.HabitPhotoAllowedCacheRepository;
import com.vladmikhayl.report.repository.ReportRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@EnableFeignClients
@SpringBootApplication
public class ReportApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReportApplication.class, args);
	}

//	@Bean
//	@Profile("!test")
//	CommandLineRunner commandLineRunner(
//			ReportRepository reportRepository,
//			HabitPhotoAllowedCacheRepository habitPhotoAllowedCacheRepository
//	) {
//		return args -> {
//			habitPhotoAllowedCacheRepository.save(
//					HabitPhotoAllowedCache.builder()
//							.habitId(1L)
//							.build()
//			);
//		};
//	}

}
