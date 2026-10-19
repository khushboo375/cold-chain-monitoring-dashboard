# Task 3 — SRS Summary

## 1. Purpose
The Cold-Chain Monitoring Dashboard is an educational web application for recording and reviewing temperature readings for cold-storage units. It highlights readings outside configured temperature limits and supports basic search and status drill-down. Sample data is used unless a sensor integration is added later.

## 2. Scope
- Enter a temperature event with unit ID, location, temperature, allowed minimum and maximum, and notes.
- View dashboard summary indicators.
- Search by unit ID, location, or note and filter by status.
- Open a detailed view for a selected event.
- View low/high temperature exceptions.
- Expose REST endpoints for events, summary metrics, and health checking.
- Build and package with Maven for later Jenkins, Selenium, Docker, and configuration-management tasks.

## 3. Functional requirements
- FR-01: The system shall accept valid temperature event data.
- FR-02: The system shall reject missing required fields and invalid temperature ranges.
- FR-03: The system shall calculate NORMAL, LOW, or HIGH status from the configured limits.
- FR-04: The dashboard shall show total readings, unique units, normal readings, and exceptions.
- FR-05: Users shall be able to search and filter readings.
- FR-06: Users shall be able to open an event detail page.
- FR-07: The exception view shall list readings outside configured limits.
- FR-08: REST endpoints shall provide event data, summary metrics, and application health.

## 4. Non-functional requirements
- The application should run locally with Java 17 and Maven.
- The application should use a simple, maintainable layered structure.
- The web interface should be usable on desktop and mobile-sized screens.
- The app should expose a health endpoint for deployment verification.
- The app should avoid committing credentials or secrets.
- This MVP is a demo, not a certified safety-monitoring system.

## 5. Assumptions and constraints
- Sample records are supplied for demonstration.
- Temperature limits are configured per event and are not universal product standards.
- No physical IoT sensor, SMS, email, or push-notification integration is included in the initial MVP.
- H2 is used for local persistence; database hardening and production operations are outside the initial scope.
