package com.vladmikhayl.habit;

import com.vladmikhayl.habit.entity.FrequencyType;
import com.vladmikhayl.habit.entity.Habit;
import com.vladmikhayl.habit.entity.SubscriptionCache;
import com.vladmikhayl.habit.entity.SubscriptionCacheId;
import com.vladmikhayl.habit.repository.HabitRepository;
import com.vladmikhayl.habit.repository.SubscriptionCacheRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@EnableFeignClients
@SpringBootApplication
public class HabitApplication {

	public static void main(String[] args) {
		SpringApplication.run(HabitApplication.class, args);
	}

	@Bean
	@Profile("!test")
	CommandLineRunner commandLineRunner(
			HabitRepository habitRepository,
			SubscriptionCacheRepository subscriptionCacheRepository
	) {
		return args -> {

//			// Юзер с ID=1 подписан на привычку с ID=1 (создана юзером user2)
//			SubscriptionCache subscriptionCache1 = SubscriptionCache.builder()
//					.id(
//							SubscriptionCacheId.builder()
//									.habitId(1L)
//									.subscriberId(1L)
//									.build()
//					)
//					.creatorLogin("user2")
//					.build();
//
//			subscriptionCacheRepository.save(subscriptionCache1);
//
//			// Юзер с ID=1 подписан на привычку с ID=3 (создана юзером user2)
//			SubscriptionCache subscriptionCache3 = SubscriptionCache.builder()
//					.id(
//							SubscriptionCacheId.builder()
//									.habitId(3L)
//									.subscriberId(1L)
//									.build()
//					)
//					.creatorLogin("user2")
//					.build();
//
//			subscriptionCacheRepository.save(subscriptionCache3);

//			Habit habit1 = Habit.builder()
//					.userId(1L)
//					.name("Вставать в 10 утра")
//					.frequencyType(FrequencyType.WEEKLY_X_TIMES)
//					.timesPerWeek(5)
//					.isPhotoAllowed(true)
//					.build();
//			habitRepository.save(habit1);
//
//			Habit habit2 = Habit.builder()
//					.userId(1L)
//					.name("Не курить")
//					.frequencyType(FrequencyType.WEEKLY_ON_DAYS)
//					.daysOfWeek(
//							Set.of(DayOfWeek.MONDAY)
//					)
//					.isHarmful(true)
//					.durationDays(30)
//					.build();
//			habitRepository.save(habit2);
		};
	}

}
