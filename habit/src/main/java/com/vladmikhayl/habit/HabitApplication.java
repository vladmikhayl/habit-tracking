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

}
