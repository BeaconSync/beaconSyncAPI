# --- build ---
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon > /dev/null
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# --- runtime ---
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --uid 1001 beaconsync
COPY --from=build /app/build/libs/*.jar app.jar
USER beaconsync
EXPOSE 8080
# Memória contida para o plano gratuito do Render; headless para tratar a foto sem tela
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+UseSerialGC", "-Djava.awt.headless=true", "-jar", "app.jar"]