package com.vladmikhayl.subscription;

import com.vladmikhayl.subscription.entity.HabitCache;
import com.vladmikhayl.subscription.repository.HabitCacheRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@EnableFeignClients
@SpringBootApplication
public class SubscriptionApplication {

	public static void main(String[] args) {
		SpringApplication.run(SubscriptionApplication.class, args);
	}

//	@Bean
//	@Profile("!test")
//	CommandLineRunner commandLineRunner(
//			HabitCacheRepository habitCacheRepository
//	) {
//		return args -> {
//			// У юзера 5 есть привычка 1
//			HabitCache habitCache = HabitCache.builder()
//					.habitId(1L)
//					.creatorId(5L)
//					.build();
//			habitCacheRepository.save(habitCache);
//		};
//	}

}
