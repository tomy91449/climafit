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
| `ms-notificaciones/` | Contactos, consentimiento y avisos por Telegram | 8084 |
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
- Swagger UI unificado: `http://localhost:8080/swagger-ui.html` (arriba a la derecha se elige el microservicio;
  para ejecutar un endpoint, botón **Authorize** con un token de Keycloak)
- Consola de Keycloak: `http://localhost:8090` (usuario `admin` / `admin`)

Para empezar de cero (borra las bases): `docker compose down -v`.

## Programar un microservicio desde el IDE

1. Abrir la carpeta del repo en IntelliJ (detecta el `pom.xml` raíz e importa los 6 proyectos).
2. Instalar la librería común en tu `.m2`: en la ventana *Maven* → `climafit-commons` → *Lifecycle* → `install`.
   Hay que repetirlo cada vez que cambia `climafit-commons`.
3. Levantar solo la infraestructura, con los puertos de las bases abiertos para tu máquina:
   `docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d postgres postgres-actividades keycloak`.
   `actividades_db` queda en `localhost:5433` y las demás bases en `localhost:5432`. En el despliegue normal
   (`docker compose up`) las bases no tienen puerto publicado: solo se llega desde la red interna de Docker.
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

## Avisos por Telegram

`ms-notificaciones` tiene dos proveedores, que se eligen con `TELEGRAM_PROVEEDOR` en el `.env`:

- `log` (por defecto): no manda nada, escribe el mensaje en el log y la notificación queda `ENVIADA`. Es el que se usa en la demo.
- `bot`: manda el aviso real con la Bot API de Telegram (`sendMessage`).

Antes los avisos iban por WhatsApp. Se cambió a Telegram porque el proveedor de WhatsApp, con una cuenta de prueba, rechazaba todos los envíos de texto libre. La Bot API de Telegram es gratuita y no tiene ese límite.

**Diferencia importante con WhatsApp:** un bot de Telegram no puede escribirle a un número de teléfono ni iniciar una conversación. Solo puede enviar a un `chat_id`, y únicamente si el usuario antes le escribió al bot. Por eso el contacto guarda `telegramChatId` en lugar de un teléfono.

### Vinculación automática

El usuario no tiene que averiguar su `chat_id`:

1. Acepta los avisos con `PUT /api/contactos/mio` y `{"aceptaTelegram": true}`.
2. La respuesta trae `linkVinculacion`, por ejemplo `https://t.me/ClimaFitBot?start=Xy7...`, con `vinculado: false`.
3. El usuario abre el link y toca **Iniciar**. El bot recibe `/start <código>`, guarda el chat y le contesta "¡Listo! Tu Telegram quedó vinculado a ClimaFit".
4. Desde ahí `GET /api/contactos/mio` muestra `vinculado: true` y el `telegramChatId`.

Detalles:
- El código es aleatorio, de un solo uso y vence a las 24 h (`climafit.telegram.vinculacion.vigencia`). Si vence, se vuelve a hacer el `PUT` y sale uno nuevo.
- Si el usuario rechaza los avisos (`aceptaTelegram: false`), el link pendiente deja de servir.
- Los mensajes del bot se leen con `getUpdates` cada 3 segundos: no hace falta una URL pública ni webhook. Solo funciona con `TELEGRAM_PROVEEDOR=bot`.
- Mientras el usuario no toca Iniciar, sus notificaciones quedan `OMITIDA` con el motivo "no tiene su chat de Telegram registrado" (RN14).
- Para pruebas se puede seguir cargando el chat a mano: `{"telegramChatId": "123456789", "aceptaTelegram": true}`.

### Pasos para el envío real

1. En Telegram, hablar con **@BotFather**, mandar `/newbot` y seguir los pasos. Al final entrega el token del bot (algo como `123456789:AA...`).
2. En el `.env`:
   ```env
   TELEGRAM_PROVEEDOR=bot
   TELEGRAM_BOT_TOKEN=123456789:AA...
   TELEGRAM_BOT_USUARIO=ClimaFitBot
   ```
   El token es un secreto: va solo en el `.env`, nunca en el código ni en el repo.
3. Recrear el servicio para que tome las variables (con `restart` no alcanza):
   `docker compose up -d --force-recreate ms-notificaciones`
4. Con el token de un alumno, hacer `PUT /api/contactos/mio` con `{"aceptaTelegram": true}`, abrir el `linkVinculacion` desde el celular y tocar **Iniciar**.
5. Inscribir al alumno a una clase y cancelarla o reprogramarla como profesor. Para seguir el envío: `docker compose logs -f ms-notificaciones`. Si salió bien aparece `Telegram enviado al chat ... (message_id ...)`.

Si el usuario nunca le escribió al bot o lo bloqueó, Telegram responde 403. La notificación se reintenta 3 veces y queda `FALLIDA` con el motivo en `ultimo_error`, sin el token.

## Convenciones

- **Capas en cada servicio:** `controlador` (HTTP) → `servicio` (reglas de negocio) → `repositorio` (datos), con `dominio` (entidades JPA) y `dto` (lo que entra y sale por la API). Las entidades nunca salen del servicio.
  `ms-actividades` / tipos de actividad es el ejemplo de referencia.
- **Errores:** lanzar `NegocioException("CODIGO", "mensaje")` (409 por defecto) o sus subclases de `climafit-commons`. El formato de respuesta lo arma la librería.
- **Seguridad:** las reglas por rol van con `@PreAuthorize`. Los controles de propiedad (RN05, RN08) se hacen en el servicio con `UsuarioActual`.
  Si un endpoint nuevo tiene un rol distinto de "cualquier autenticado", agregar también la regla gruesa en
  `api-gateway/.../seguridad/SeguridadGatewayConfig.java` y su caso en `SeguridadGatewayTest`. Las rutas `/internal/**` el gateway las bloquea solo (404).
- **Base de datos:** solo con migraciones Flyway (`src/main/resources/db/migration/V<n>__descripcion.sql`). Una migración ya subida no se edita: se agrega una nueva.
- **Git:** nadie sube directo a `main`. Cada tarea va en una rama (`feature/ms-clima-evaluacion`) con un Pull Request que revisa otro integrante.
