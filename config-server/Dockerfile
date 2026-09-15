FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY config-server/pom.xml .
RUN mvn -q -B dependency:go-offline
COPY config-server/src ./src
RUN mvn -q -B -DskipTests clean package

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache git
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

COPY config-repo /config-repo
RUN git config --global user.email "nikolina.l97@gmail.com" \
 && git config --global user.name "nikolina121" \
 && git config --global --add safe.directory /config-repo \
 && cd /config-repo \
 && git init -q \
 && git add . \
 && git commit -q -m "initial config"

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
