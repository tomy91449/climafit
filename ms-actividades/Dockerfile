# =====================================================================================
# Dockerfile de ms-actividades: la RECETA para armar su imagen de Docker.
#
# Para principiantes:
#  - Una IMAGEN es como un "paquete" que trae todo lo necesario para correr el programa
#    (sistema operativo mínimo, Java y el .jar de ms-actividades).
#  - Un CONTENEDOR es esa imagen funcionando. docker-compose.yml crea un contenedor por servicio.
#  - Cada línea de abajo es un paso de la receta. Docker los ejecuta en orden.
#
# Build "multi-etapa" (2 etapas):
#  1) Una imagen grande, con Maven y Java, COMPILA el código y genera el .jar.
#  2) Una imagen chica, solo con Java para EJECUTAR, recibe ese .jar. Lo de la etapa 1 se descarta.
#  Así la imagen final es liviana y no lleva ni el código fuente ni las herramientas de compilación.
#
# El contexto es la RAÍZ del repo (ver "build" en docker-compose.yml), para poder
# compilar primero climafit-commons y después ms-actividades, que la usa como dependencia.
# Así la imagen se construye sin depender de lo que haya instalado en la máquina.
# =====================================================================================

# ---- Etapa 1: compilar ----
# FROM = imagen de partida. Esta trae Maven 3.9 y Java 21. "AS build" le pone nombre a la etapa.
FROM maven:3.9-eclipse-temurin-21 AS build
# WORKDIR = carpeta de trabajo dentro de la imagen (como hacer "cd /build").
WORKDIR /build
# 1) Instalar la librería común: COPY copia la carpeta del repo a la imagen y RUN ejecuta Maven.
#    "install" la deja en el repositorio local de Maven (.m2) para que ms-actividades la encuentre.
#    --mount=type=cache guarda las librerías descargadas entre builds: la segunda vez es mucho más rápido.
COPY climafit-commons climafit-commons
RUN --mount=type=cache,target=/root/.m2,sharing=locked mvn -q -f climafit-commons/pom.xml install -DskipTests
# 2) Compilar ms-actividades: "package" genera el .jar en ms-actividades/target/.
#    -DskipTests = no corre los tests acá (se corren aparte, en el IDE o con Maven).
COPY ms-actividades ms-actividades
RUN --mount=type=cache,target=/root/.m2,sharing=locked mvn -q -f ms-actividades/pom.xml package -DskipTests

# ---- Etapa 2: imagen final, solo con el JRE y el .jar ----
# JRE = solo lo necesario para EJECUTAR Java (no para compilar): imagen mucho más chica.
FROM eclipse-temurin:21-jre
WORKDIR /app
# Se trae SOLO el .jar desde la etapa "build" y se lo renombra app.jar.
COPY --from=build /build/ms-actividades/target/ms-actividades-*.jar app.jar
# EXPOSE documenta que adentro del contenedor el servicio escucha en el puerto 8080.
# (Abrirlo hacia afuera, o no, lo decide docker-compose.yml con "ports".)
EXPOSE 8080
# ENTRYPOINT = el comando que se ejecuta al arrancar el contenedor.
ENTRYPOINT ["java", "-jar", "app.jar"]
