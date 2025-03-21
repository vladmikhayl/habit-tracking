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
					.createdAt(LocalDateTime.now())
					.build();
			habitRepository.save(habit1);
		};
	}

}
