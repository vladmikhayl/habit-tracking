package com.vladmikhayl.habit;

import com.vladmikhayl.habit.entity.FrequencyType;
import com.vladmikhayl.habit.entity.Habit;
import com.vladmikhayl.habit.repository.HabitRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@SpringBootApplication
public class HabitApplication {

	public static void main(String[] args) {
		SpringApplication.run(HabitApplication.class, args);
	}

	@Bean
	CommandLineRunner commandLineRunner(
			HabitRepository habitRepository
	) {
		return args -> {
			Habit habit1 = Habit.builder()
					.userId(1L)
					.name("Вставать в 10 утра")
					.frequencyType(FrequencyType.WEEKLY_X_TIMES)
					.timesPerWeek(5)
					.isPhotoAllowed(true)
					.build();
			habitRepository.save(habit1);

			Habit habit2 = Habit.builder()
					.userId(1L)
					.name("Не курить")
					.frequencyType(FrequencyType.WEEKLY_ON_DAYS)
					.daysOfWeek(
							Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SUNDAY, DayOfWeek.SATURDAY)
					)
					.isHarmful(true)
					.durationDays(30)
					.build();
			habitRepository.save(habit2);
		};
	}

}
