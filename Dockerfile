FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven && mvn package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S fasol && adduser -S fasol -G fasol
COPY --from=builder /app/target/*.jar app.jar
USER fasol
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
