# Build multi-etapa. El contexto es la RAÍZ del repo (ver docker-compose.yml), para poder
# compilar primero climafit-commons y después el microservicio que depende de ella.
# Así la imagen se construye sin depender de lo que haya instalado en la máquina.

# ---- Etapa 1: compilar ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
# 1) Instalar la librería común en el .m2 de la imagen de build
COPY climafit-commons climafit-commons
RUN --mount=type=cache,target=/root/.m2,sharing=locked mvn -q -f climafit-commons/pom.xml install -DskipTests
# 2) Compilar el microservicio (usa la librería recién instalada como dependencia Maven)
COPY ms-clases ms-clases
RUN --mount=type=cache,target=/root/.m2,sharing=locked mvn -q -f ms-clases/pom.xml package -DskipTests

# ---- Etapa 2: imagen final, solo con el JRE y el .jar ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/ms-clases/target/ms-clases-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
