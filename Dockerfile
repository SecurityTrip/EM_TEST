# Этап сборки (build)
FROM maven:3.9.6-eclipse-temurin-21 AS builder

# Устанавливаем рабочую директорию внутри контейнера
WORKDIR /app

# Копируем pom.xml и загружаем зависимости (для кэширования)
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# Копируем остальной исходный код
COPY src ./src

# Собираем jar-файл, исключая тесты
RUN mvn clean package -DskipTests

# Этап выполнения (runtime)
FROM eclipse-temurin:21-jre-jammy

# Создаем рабочую директорию
WORKDIR /app

# Копируем собранный jar-файл из builder
COPY --from=builder /app/target/*.jar app.jar

# Открываем порт приложения
EXPOSE 8080

# Запускаем Spring Boot приложение
ENTRYPOINT ["java", "-jar", "app.jar"]
