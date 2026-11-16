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

## WhatsApp con Twilio

`ms-notificaciones` tiene dos proveedores, que se eligen con `WHATSAPP_PROVEEDOR` en el `.env`:

- `log` (por defecto): no manda nada, escribe el mensaje en el log y la notificación queda `ENVIADA`. Es el que se usa en la demo.
- `twilio`: manda el aviso real por el sandbox de WhatsApp de Twilio.

### Estado actual (16/11/2026): la cuenta Trial no puede enviar

Se probó la integración de punta a punta y el código funciona: al cancelar una clase, ms-clases le pide el aviso a ms-notificaciones, que llama a Twilio, reintenta 3 veces y deja la notificación `FALLIDA` con el error de Twilio en `ultimo_error`.

Twilio rechaza todos los envíos de una cuenta **Trial**:

| Prueba | Respuesta de Twilio |
|---|---|
| Texto libre desde el sandbox, incluso con la ventana de 24 h abierta | `21654 ContentSid Required` |
| Plantilla de ejemplo `HXb5b6…` ("Your appointment is coming up…") | `21655 The ContentSid is Invalid` |
| El botón *Send template message* de la propia consola de Twilio | `21655 The ContentSid is Invalid` |

Solo llegan las respuestas automáticas del sandbox ("You are all set", "You said: …"). Para que llegue el aviso real hay que pasar la cuenta a paga (Upgrade). No hace falta tocar código.

### Pasos para usar Twilio (con cuenta paga)

1. En el `.env` completar los valores (el SID y el token están en la página principal de la consola de Twilio):
   ```env
   WHATSAPP_PROVEEDOR=twilio
   TWILIO_ACCOUNT_SID=AC...
   TWILIO_AUTH_TOKEN=...
   TWILIO_FROM=+14155238886
   ```
   El número va en formato E.164, sin espacios y sin el prefijo `whatsapp:` (lo agrega el código).
2. Recrear el servicio para que tome las variables (con `restart` no alcanza):
   `docker compose up -d --force-recreate ms-notificaciones`
3. Desde el celular que va a recibir los avisos, mandar por WhatsApp `join <código>` al +1 415 523 8886. El código está en la consola, en *Messaging → Try it out → Send a WhatsApp message*. La unión dura 72 h.
4. Antes de probar, mandar cualquier mensaje desde ese celular al sandbox: así se abre la ventana de 24 h en la que se puede mandar texto libre.
5. Registrar el teléfono del alumno con `PUT /api/contactos/mio`. Los celulares argentinos llevan el 9: `{"telefono": "+5493511234567", "aceptaWhatsapp": true}`.
6. Inscribir al alumno a una clase y cancelarla o reprogramarla como profesor. Para seguir el envío: `docker compose logs -f ms-notificaciones`. Si salió bien aparece `WhatsApp enviado por Twilio a ... (sid SM...)`.

No usar el entorno *Try out WhatsApp* (+1 737 250 8034, código `join twilio-trial`): solo acepta plantillas predefinidas, nunca texto libre.

## Convenciones

- **Capas en cada servicio:** `controlador` (HTTP) → `servicio` (reglas de negocio) → `repositorio` (datos), con `dominio` (entidades JPA) y `dto` (lo que entra y sale por la API). Las entidades nunca salen del servicio.
  `ms-actividades` / tipos de actividad es el ejemplo de referencia.
- **Errores:** lanzar `NegocioException("CODIGO", "mensaje")` (409 por defecto) o sus subclases de `climafit-commons`. El formato de respuesta lo arma la librería.
- **Seguridad:** las reglas por rol van con `@PreAuthorize`. Los controles de propiedad (RN05, RN08) se hacen en el servicio con `UsuarioActual`.
- **Base de datos:** solo con migraciones Flyway (`src/main/resources/db/migration/V<n>__descripcion.sql`). Una migración ya subida no se edita: se agrega una nueva.
- **Git:** nadie sube directo a `main`. Cada tarea va en una rama (`feature/ms-clima-evaluacion`) con un Pull Request que revisa otro integrante.
