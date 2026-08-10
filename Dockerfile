# syntax=docker/dockerfile:1

FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/
# maven.wagon.http.pool=false and maxPerRoute=1 avoid connection-pool exhaustion against
# Maven Central seen on some constrained/virtualized Docker hosts (e.g. colima on macOS).
RUN ./mvnw clean package -DskipTests \
    -Dmaven.wagon.http.pool=false \
    -Dhttp.keepAlive=false \
    -Dmaven.wagon.httpconnectionManager.maxPerRoute=1

FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]
