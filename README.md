# Instructions for candidates

This is the Java version of the Payment Gateway challenge. If you haven't already read this [README.md](https://github.com/cko-recruitment/) on the details of this exercise, please do so now.

## Requirements
- JDK 17
- Docker
- Maven (or use the included `./mvnw` wrapper — no local install required)

## Build tool

The original template scaffolded this project with Gradle (`build.gradle`, `gradlew`). This
solution uses **Maven** instead (`pom.xml`, `mvnw`) — the README for this exercise explicitly
allows changing structure/libraries, so the Gradle files have been removed rather than left
alongside an unused, out-of-date build config. Run tests with `./mvnw test`, run the app with
`./mvnw spring-boot:run`.

## Template structure

src/ - The Spring Boot application (originally a skeleton, now the full solution)

imposters/ - contains the bank simulator configuration. Don't change this

.editorconfig - don't change this. It ensures a consistent set of rules for submissions when reformatting code

docker-compose.yml - configures the bank simulator

## API Documentation
For documentation openAPI is included, and it can be found under the following url: **http://localhost:8090/swagger-ui/index.html**

## Running with Docker

A separate `Dockerfile` (multi-stage: Maven/JDK build stage, slim JRE runtime stage) packages
this app on its own — it's independent of `docker-compose.yml`, which only runs the bank
simulator. Start the bank simulator first, then build and run the app image:

```
docker compose up -d
docker build -t payment-gateway .
docker run --rm -p 8090:8090 --network host payment-gateway
```

`--network host` lets the containerized app reach the bank simulator at
`http://localhost:8080` (its default `bank-simulator.base-url`) without extra networking setup.
On Docker Desktop for Mac/Windows, where `--network host` isn't supported the same way, use
`-e BANK_SIMULATOR_BASE_URL=http://host.docker.internal:8080` instead of `--network host`.

**Feel free to change the structure of the solution, use a different library etc.**