FROM maven:3.9.9-eclipse-temurin-17-alpine AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S foodorder && adduser -S foodorder -G foodorder
WORKDIR /app
COPY --from=build /workspace/target/foodorder-api-1.0.0.jar app.jar
USER foodorder
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
