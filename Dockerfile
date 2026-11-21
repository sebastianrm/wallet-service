# ---------------------------------------------------------------------------
# Imagen multi-etapa del microservicio.
# Etapa 1: compilacion con Maven + JDK 17.
# Etapa 2: runtime minimo con JRE 17 sobre Alpine.
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S yanki && adduser -S yanki -G yanki
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
RUN chown -R yanki:yanki /app
USER yanki
EXPOSE 8087
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]