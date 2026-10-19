# Cold-Chain Monitoring Dashboard

A student DevOps mini project: a simple dashboard for recording temperature events, searching records, reviewing summary indicators, viewing event details, and identifying out-of-range readings. The application uses sample/demo data and is not connected to live sensors.

## Technology stack
- Java 17
- Spring Boot 3.5.5 / Spring MVC
- Thymeleaf + HTML/CSS
- Spring Data JPA
- H2 file database
- Maven
- Embedded Tomcat
- JUnit / Selenium planned for later tasks

## Prerequisites
1. JDK 17 installed.
2. Apache Maven installed.
3. Git recommended.
4. Internet access on the first build so Maven can download dependencies.

Check the tools in Command Prompt or PowerShell:

```powershell
java -version
mvn -version
git --version
```

The Java output should report version 17. Maven must be available as `mvn`.

## Run locally on Windows

1. Extract the project ZIP.
2. Open Command Prompt or PowerShell in the extracted `cold-chain-monitoring-dashboard` folder (the folder containing `pom.xml`).
3. Build and test:

```powershell
mvn clean test
```

4. Start the application:

```powershell
mvn spring-boot:run
```

5. Open these URLs in your browser:
   - Dashboard: http://localhost:8089/
   - Alerts: http://localhost:8089/alerts
   - Health: http://localhost:8089/health
   - Events API: http://localhost:8089/api/events
   - Summary API: http://localhost:8089/api/summary

To stop the app, press `Ctrl+C` in the terminal.

## Run using a packaged JAR

```powershell
mvn clean package
java -jar target/cold-chain-dashboard.jar
```

To use a different port:

```powershell
$env:APP_PORT=8081
mvn spring-boot:run
```

Then open http://localhost:8081/.

## H2 database
The local database files are created under `data/`. The development-only H2 console is at http://localhost:8080/h2-console. Use JDBC URL `jdbc:h2:file:./data/coldchain`, username `sa`, and a blank password. Do not expose the H2 console to a public network.

## Quick acceptance checks
1. Dashboard loads and displays sample records.
2. Summary indicators display totals and exceptions.
3. Search `VAX-001` and confirm matching results.
4. Filter status to `HIGH` or `LOW`.
5. Open a row's Details link.
6. Add a reading with temperature `10`, minimum `2`, and maximum `8`; it should be marked `HIGH`.
7. Open Alerts / Exceptions and confirm out-of-range readings appear.
8. Open `/health` and confirm the JSON status is `UP`.
9. Open `/api/summary` and confirm summary values are returned.

## Project structure

```text
cold-chain-monitoring-dashboard/
├── pom.xml
├── README.md
├── docs/
│   ├── task-3-srs-summary.md
│   ├── task-3-use-cases.md
│   ├── task-3-architecture.md
│   └── task-3-data-model-and-api.md
└── src/
    ├── main/
    │   ├── java/com/coldchain/dashboard/
    │   │   ├── ColdChainDashboardApplication.java
    │   │   ├── config/SampleDataLoader.java
    │   │   ├── controller/DashboardController.java
    │   │   ├── model/ColdChainEvent.java
    │   │   └── repository/ColdChainEventRepository.java
    │   └── resources/
    │       ├── application.properties
    │       ├── static/css/style.css
    │       └── templates/
    └── test/java/com/coldchain/dashboard/
```

## Scope and limitations
- This is an MVP using sample data, not real-time IoT sensor telemetry.
- Alerts are displayed in the application; no SMS/email/push notifications are sent.
- The configured limits are demonstration values, not universal safety recommendations.
- Authentication, role-based access, production database hardening, and deployment automation will be considered only if required by later tasks.
