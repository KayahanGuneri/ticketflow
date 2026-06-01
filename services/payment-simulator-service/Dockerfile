FROM eclipse-temurin:21-jre

WORKDIR /app

ARG JAR_FILE=target/payment-simulator-service-0.0.1-SNAPSHOT.jar

COPY ${JAR_FILE} app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app/app.jar"]