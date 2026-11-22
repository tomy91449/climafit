#!/bin/bash
# Se ejecuta UNA sola vez, cuando el volumen de PostgreSQL está vacío.
# Crea una base y un usuario por componente. Cada usuario es dueño solo de su base (no es
# superusuario), así un microservicio no puede leer ni escribir los datos de otro.
#
# El mismo script lo usan los dos contenedores de PostgreSQL. Cada contenedor crea solo las bases
# cuya contraseña recibe por variable de entorno (ver docker-compose.yml):
#   postgres             -> clases_db, notificaciones_db, keycloak_db
#   postgres-actividades -> actividades_db
set -euo pipefail

crear_base() {
  local base="$1" usuario="$2" password="$3"
  if [ -z "$password" ]; then
    return 0 # esta base no va en este contenedor
  fi
  echo "Creando la base ${base} (dueño: ${usuario})"
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
