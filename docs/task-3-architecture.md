# Task 3 — Application architecture

```mermaid
flowchart TD
    User[Operator / Manager] --> Browser[Web browser]
    Browser --> MVC[Spring MVC Controller]
    MVC --> Service[Business rules / status calculation]
    Service --> Repo[Spring Data JPA Repository]
    Repo --> DB[(H2 file database)]
    MVC --> Views[Thymeleaf templates + CSS]
    Views --> Browser
    Client[REST API client / Selenium tests] --> API[REST endpoints]
    API --> Service
    Jenkins[Jenkins pipeline - later task] --> Maven[Maven build and tests]
    Maven --> JAR[Executable JAR with embedded Tomcat]
    JAR --> App[Cold-Chain application]
    App --> DB
```

## Design notes
- **Presentation:** Thymeleaf templates and CSS.
- **Controller:** Handles page requests and REST endpoints.
- **Business logic:** Derives status by comparing a reading with its configured minimum and maximum.
- **Persistence:** Spring Data JPA writes to an H2 file database.
- **Deployment:** Maven packages an executable JAR with embedded Tomcat. Jenkins, Docker, and Ansible will be introduced in later tasks.
