FROM eclipse-temurin:21-jre

WORKDIR /app

COPY target/cold-chain-dashboard.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar", "--server.port=8080"]