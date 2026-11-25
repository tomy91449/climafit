#!/bin/bash
# Se ejecuta UNA sola vez, cuando el volumen de PostgreSQL está vacío.
# Crea una base y un usuario por componente. Cada usuario es dueño solo de su base (no es
# superusuario), así un microservicio no puede leer ni escribir los datos de otro.
#
# Para principiantes: la imagen oficial de PostgreSQL ejecuta automáticamente los scripts que
# encuentra en /docker-entrypoint-initdb.d/ la primera vez que arranca con la base vacía.
# docker-compose.yml pone este archivo ahí. Si la base ya existe (volumen con datos), no se ejecuta;
# para que vuelva a correr hay que borrar los datos con "docker compose down -v".
#
# El mismo script lo usan los dos contenedores de PostgreSQL. Cada contenedor crea solo las bases
# cuya contraseña recibe por variable de entorno (ver docker-compose.yml):
#   postgres             -> clases_db, notificaciones_db, keycloak_db
#   postgres-actividades -> actividades_db

# Cortar ante el primer error (así no queda una base a medio crear sin que nadie se entere).
set -euo pipefail

# crear_base <nombre de la base> <usuario dueño> <contraseña>
crear_base() {
  local base="$1" usuario="$2" password="$3"
  if [ -z "$password" ]; then
    return 0 # esta base no va en este contenedor
  fi
  echo "Creando la base ${base} (dueño: ${usuario})"
  # Crea el usuario, crea la base con ese usuario como dueño y le saca el acceso a todos los demás.
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres <<-SQL
    CREATE USER ${usuario} WITH PASSWORD '${password}';
    CREATE DATABASE ${base} OWNER ${usuario};
    REVOKE ALL ON DATABASE ${base} FROM PUBLIC;
SQL
}

crear_base actividades_db    actividades    "${ACTIVIDADES_DB_PASSWORD:-}"
crear_base clases_db         clases         "${CLASES_DB_PASSWORD:-}"
crear_base notificaciones_db notificaciones "${NOTIFICACIONES_DB_PASSWORD:-}"
crear_base keycloak_db       keycloak       "${KEYCLOAK_DB_PASSWORD:-}"
