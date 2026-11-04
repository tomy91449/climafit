# ClimaFit

Backend para clases deportivas al aire libre que se suspenden, reprograman y notifican según el pronóstico del clima.
TPI 2026 — Backend de Aplicaciones, UTN FRC.

La propuesta completa (funcionalidades F01–F15, reglas RN01–RN16, diagramas y compromiso) está en [`docs/propuesta.md`](docs/propuesta.md).

## Estructura

| Carpeta | Qué es | Puerto local (IDE) |
|---|---|---|
| `climafit-commons/` | Librería JAR común: formato de errores, seguridad JWT/roles, correlation id | — |
| `api-gateway/` | Punto de entrada único (`/api/**`) con Security Filter | 8080 |
| `ms-actividades/` | Catálogo: tipos de actividad y criterios climáticos, espacios, ejercicios | 8081 |
| `ms-clases/` | Clases, inscripciones, estados, historial; orquesta la evaluación climática | 8082 |
| `ms-clima/` | Pronóstico de Open-Meteo y evaluación APTA / EN_RIESGO / NO_APTA | 8083 |
| `ms-notificaciones/` | Contactos, consentimiento y avisos por WhatsApp | 8084 |
| `infra/` | Script de bases de PostgreSQL y realm de Keycloak | — |
| `postman/` | Colección y entorno de Postman | — |

## Requisitos

- Docker Desktop.
- Para programar: IntelliJ con **JDK 21** (*File → Project Structure → SDKs → Download JDK → Temurin 21*).
  Maven no hace falta instalarlo: IntelliJ trae uno.

## Levantar todo con Docker

```bash
cp .env.example .env          # los valores por defecto sirven
docker compose up -d --build  # la primera vez tarda unos minutos
docker compose ps             # esperar a que todo diga (healthy)
```

- API: `http://localhost:8080/api/...`
- Consola de Keycloak: `http://localhost:8090` (usuario `admin` / `admin`)

Para empezar de cero (borra las bases): `docker compose down -v`.

## Programar un microservicio desde el IDE

1. Abrir la carpeta del repo en IntelliJ (detecta el `pom.xml` raíz e importa los 6 proyectos).
2. Instalar la librería común en tu `.m2`: en la ventana *Maven* → `climafit-commons` → *Lifecycle* → `install`.
   Hay que repetirlo cada vez que cambia `climafit-commons`.
3. Levantar solo la infraestructura: `docker compose up -d postgres keycloak`.
4. Ejecutar la clase `*Application` del servicio. Los valores por defecto de su `application.yml` apuntan a `localhost`.

Si querés probar tu servicio detrás del gateway, frená su contenedor (`docker compose stop ms-xxx`) y levantá el gateway desde el IDE también.

## Usuarios de prueba (realm `climafit`)

| Usuario | Contraseña | Rol |
|---|---|---|
| `admin1` | `admin123` | ADMIN |
| `profe1`, `profe2` | `profe123` | PROFESOR |
| `alumno1`, `alumno2`, `alumno3` | `alumno123` | ALUMNO |

Además existe el cliente `climafit-servicios` (client credentials, rol SERVICIO) para las llamadas entre microservicios.

Obtener un token:

```bash
curl -s -d grant_type=password -d client_id=climafit-api -d username=profe1 -d password=profe123 \
  http://localhost:8090/realms/climafit/protocol/openid-connect/token
```

y usarlo como `Authorization: Bearer <access_token>`.

## Convenciones

- **Capas en cada servicio:** `controlador` (HTTP) → `servicio` (reglas de negocio) → `repositorio` (datos), con `dominio` (entidades JPA) y `dto` (lo que entra y sale por la API). Las entidades nunca salen del servicio.
  `ms-actividades` / tipos de actividad es el ejemplo de referencia.
- **Errores:** lanzar `NegocioException("CODIGO", "mensaje")` (409 por defecto) o sus subclases de `climafit-commons`. El formato de respuesta lo arma la librería.
- **Seguridad:** las reglas por rol van con `@PreAuthorize`. Los controles de propiedad (RN05, RN08) se hacen en el servicio con `UsuarioActual`.
- **Base de datos:** solo con migraciones Flyway (`src/main/resources/db/migration/V<n>__descripcion.sql`). Una migración ya subida no se edita: se agrega una nueva.
- **Git:** nadie sube directo a `main`. Cada tarea va en una rama (`feature/ms-clima-evaluacion`) con un Pull Request que revisa otro integrante.
