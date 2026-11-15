# El gateway no depende de climafit-commons, así que se compila solo.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY api-gateway api-gateway
RUN --mount=type=cache,target=/root/.m2,sharing=locked mvn -q -f api-gateway/pom.xml package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/api-gateway/target/api-gateway-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
