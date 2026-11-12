# Imagen del backend UCO Kudos (multi-etapa, usuario sin privilegios)
# Nota: verifica que las etiquetas existan en Docker Hub; si no, usa la LTS equivalente.
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:25-jre
RUN addgroup --system kudos && adduser --system --ingroup kudos kudos
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
USER kudos
EXPOSE 8080
# Render inyecta PORT; application.yml usa server.port=${PORT:8080}
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
