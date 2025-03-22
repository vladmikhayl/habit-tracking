package com.vladmikhayl.habit;

import com.vladmikhayl.habit.entity.Habit;
import com.vladmikhayl.habit.repository.HabitRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;

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
					.frequency("Ежедневно")
					.build();
			habitRepository.save(habit1);

			Habit habit2 = Habit.builder()
					.userId(1L)
					.name("Ложиться в 1 час ночи")
					.frequency("Ежедневно")
					.photoAllowed(true)
					.durationDays(30)
					.build();
			habitRepository.save(habit2);
		};
	}

}
