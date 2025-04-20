# Шаг 1. Сборка проекта
FROM maven:3.9.6-eclipse-temurin-17 AS build

# Рабочая директория
WORKDIR /build

# Копируем весь проект
COPY . .

# Собираем только нужный модуль
RUN mvn clean package -DskipTests -pl subscription -am

# Шаг 2. Запуск
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# Копируем сгенерированный .jar
COPY --from=build /build/subscription/target/subscription-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
