#!/bin/bash
# Se ejecuta UNA sola vez, cuando el volumen de PostgreSQL está vacío.
# Crea una base y un usuario por componente. Cada usuario es dueño solo de su base,
# así un microservicio no puede leer ni escribir los datos de otro.
set -euo pipefail

crear_base() {
  local base="$1" usuario="$2" password="$3"
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres <<-SQL
    CREATE USER ${usuario} WITH PASSWORD '${password}';
    CREATE DATABASE ${base} OWNER ${usuario};
    REVOKE ALL ON DATABASE ${base} FROM PUBLIC;
SQL
}

crear_base actividades_db    actividades    "$ACTIVIDADES_DB_PASSWORD"
crear_base clases_db         clases         "$CLASES_DB_PASSWORD"
crear_base notificaciones_db notificaciones "$NOTIFICACIONES_DB_PASSWORD"
crear_base keycloak_db       keycloak       "$KEYCLOAK_DB_PASSWORD"
