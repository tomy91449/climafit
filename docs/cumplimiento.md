# ClimaFit — Matriz de cumplimiento (Fase 3)

La entrega está lista cuando cada fila tiene su evidencia funcionando (plan de trabajo, Fase 3).
La mantiene la Persona 2; cada responsable revisa su fila.

**Última verificación:** 15/11/2026, sobre `main` en el commit `e24f585`, con `docker compose` levantado desde cero.

- Contenedores: los 7 quedan `healthy` (postgres, keycloak, api-gateway y los 4 microservicios).
- Colección de Postman completa: **74 pedidos y 114 verificaciones, 0 fallas**.
- `mvn clean verify`: **281 tests, 0 fallas** (commons 24, ms-actividades 47, ms-clases 150, ms-clima 46, ms-notificaciones 14).

Cómo repetirla:

1. `cp .env.example .env` y `docker compose up -d --build` desde un entorno limpio (`docker compose down -v` antes).
2. Esperar a que `docker compose ps` muestre todo `healthy`.
3. Correr la colección `postman/ClimaFit.postman_collection.json` completa y en orden, con el entorno
   `postman/ClimaFit-local.postman_environment.json` (Collection Runner o `newman run`).
4. `mvn clean verify` desde la raíz para los tests automáticos.

Estados: ✅ verificado · ⚠️ funciona pero con una observación · ⏳ pendiente.

## 1. Requisitos del enunciado (propuesta, sección 11.1)

| Requisito | Responsable | Evidencia | Estado |
|---|---|---|---|
| API Gateway con Security Filter | Persona 1 | Carpeta 01 de Postman: 401 sin token y con token inválido, 403 por rol, 404 en ruta interna | ✅ |
| 4 microservicios en Spring Boot con colaboración real | Personas 3 y 4 | Carpeta 06: ms-clases usa ms-actividades, ms-clima y ms-notificaciones en el mismo proceso | ✅ |
| Keycloak con OIDC, JWT y roles | Persona 1 | Carpeta 00: tokens de los 6 usuarios; token de servicio entre microservicios | ✅ |
| API pública externa (Open-Meteo) en la decisión de negocio | Persona 4 | Carpeta 10: pronóstico real en modo REAL | ✅ |
| Librería `climafit-commons` en `.m2`, usada por 4 microservicios | Persona 2 | Dependencia en los 4 `pom.xml`; Dockerfiles multi-etapa que la instalan antes de compilar | ✅ |
| PostgreSQL, una base por servicio | Persona 1 | `infra/postgres/init.sh`: `actividades_db`, `clases_db`, `notificaciones_db`, `keycloak_db`, con un usuario por base | ✅ |
| APIs REST documentadas con OpenAPI | Cada dueño; Persona 1 el Swagger unificado | `/v3/api-docs` de cada servicio: 39 endpoints (15 + 15 + 5 + 4) | ⚠️ ver observación 2 |
| Despliegue completo con Docker Compose | Persona 1 | CP7: los 7 contenedores quedan `healthy` sin pasos manuales | ✅ |

## 2. Reglas de negocio

| Regla | Responsable | Tests automáticos | Pedido de Postman | Estado |
|---|---|---|---|---|
| RN01 Cupo máximo | Persona 3 | ms-clases: 3 tests `rn01_*` (`InscripcionServiceTest`, `ClaseControllerTest`) | 04: alumno 3 en A con cupo completo, 409 | ✅ |
| RN02 Clase disponible | Persona 3 | ms-clases: 7 tests `rn02_*` (`InscripcionServiceTest`) | 09: inscripción en clase cancelada, 409 | ✅ |
| RN03 Superposición del alumno | Persona 3 | ms-clases: 3 tests `rn03_*` (`InscripcionServiceTest`) | 04: alumno 1 en E, 409 | ✅ |
| RN04 Superposición del profesor | Persona 3 | ms-clases: 6 tests `rn04_*` (`ClaseServiceTest`, `ClaseControllerTest`) | 04: otra clase en el mismo horario, 409 | ✅ |
| RN05 Autorización sobre la clase | Personas 3 y 4 | ms-clases: 10 tests `rn05_*`; ms-notificaciones: `rn05UnProfesorNoVeLasNotificacionesDeClasesAjenas` | 07 y 09: decisión y cancelación ajenas, 403 | ✅ |
| RN06 Cancelación y clases pasadas | Persona 3 | ms-clases: 5 tests `rn06_*` (`ClaseServiceTest`, `CambioEstadoServiceTest`) | 09: cancelación de E | ✅ |
| RN07 Compatibilidad de ejercicios | Personas 2 y 3 | ms-actividades: 9 tests `rn07_*`; ms-clases: 3 tests `rn07_*` | 04: ejercicio incompatible, 422 | ✅ |
| RN08 Propiedad de los ejercicios | Persona 2 | ms-actividades: 14 tests `rn08_*` (`EjercicioServiceTest`, `EjerciciosTest`) | 02: ejercicio ajeno, 404 | ✅ |
| RN09 Clases a evaluar | Persona 4 | ms-clases: 5 tests `rn09_*` (`EvaluacionServiceTest`) | 09: clase techada, 409 | ✅ |
| RN10 Evaluación del pronóstico | Persona 4 | ms-clima: 22 tests `rn10_*` (`EvaluadorClimaticoTest`) | 06 y 07 | ⚠️ ver observación 1 |
| RN11 Decisión según el resultado | Persona 4 | ms-clases: 6 tests `rn11_*` (`EvaluacionServiceTest`) | 06 y 07 | ✅ |
| RN12 Clase en riesgo | Persona 4 | ms-clases: 3 tests `rn12_*` (`DecisionRiesgoServiceTest`) | 07, y 09 con 409 `CLASE_NO_EN_RIESGO` | ✅ |
| RN13 Búsqueda de nuevo día | Persona 4 | ms-clases: 10 tests `rn13_*` (`ReprogramacionServiceTest`) | 06 (día siguiente) y 08 (sin día viable) | ✅ |
| RN14 Consentimiento para WhatsApp | Persona 1 | ms-notificaciones: `rn14SeEnviaAlQueAceptoYSeOmiteAlQueNo`, `omiteSiElAlumnoNoAceptoWhatsapp`, `omiteSiElUsuarioNuncaRegistroSuContacto` | 06: notificación OMITIDA del alumno 2 | ✅ |
| RN15 Notificación única por evento | Persona 1 | ms-notificaciones: test de integración `rn15ElMismoEventoRepetidoNoDuplicaNotificaciones` | 06: sin duplicados | ✅ |
| RN16 Falla de servicios | Personas 1, 3 y 4 | ms-clases: 10 tests `rn16_*`; ms-clima: 2 tests `rn16_*`; ms-notificaciones: `despuesDeTresFallasQuedaFallida`, `reintentaYSiElSegundoIntentoFuncionaQuedaEnviada` | 05: evaluación FALLIDA | ✅ |

## 3. Comprobaciones observables (propuesta, sección 11.2)

| Comprobación | Responsable | Evidencia | Estado |
|---|---|---|---|
| CP1 Seguridad y roles | Persona 1 | Carpeta 01 | ✅ |
| CP2 Inscripción con cupo y superposición | Persona 3 | Carpeta 04 | ✅ |
| CP3 Proceso central: suspensión y reprogramación automática | Persona 4, con 3 | Carpeta 06 | ✅ |
| CP4 Clase en riesgo y decisión del profesor | Persona 4 | Carpeta 07 (viento de 30 km/h, límite de Yoga 35 km/h) | ✅ |
| CP5 Sin día viable y reagenda manual | Persona 3, con 4 | Carpeta 08 | ✅ |
| CP6 Falla del servicio de clima | Persona 4 | Carpeta 05 | ✅ |
| CP7 Despliegue e integración real | Persona 1, con 2 | `docker compose up -d --build` desde cero + carpeta 10 | ⚠️ falta el Swagger UI unificado (observación 2) |

## 4. Observaciones abiertas

1. **RN10 no coincide con lo acordado** (Persona 4). `EvaluadorClimatico` de ms-clima todavía da NO_APTA
   solo por probabilidad de lluvia y mantiene el riesgo "a 10 puntos". La regla acordada está en
   [contratos.md](contratos.md), sección 6.2, y en la propuesta. Las carpetas 06 a 08 pasan igual porque
   sus escenarios dan el mismo resultado con las dos reglas; hay que ajustar el evaluador y sus tests `rn10_*`.
2. **Swagger UI unificado** (Persona 1). Cada microservicio publica su `/v3/api-docs` con sus endpoints,
   pero el gateway no tiene el Swagger UI unificado de la propuesta: `http://localhost:8080/swagger-ui.html`
   responde 401 y `api-gateway` no incluye springdoc (plan, tarea 1.4). CP7 lo pide en su resultado esperado.
3. **Nombres de tests de ms-notificaciones** (Persona 1). El plan pide `rn14_...`; hoy son `rn14Se...` o no
   llevan el código de la regla. Funcionan, pero conviene renombrarlos para que se encuentren por regla.
4. **Envío real por Twilio** (Persona 1). Solo se verificó el proveedor `log`. Falta la prueba con
   `WHATSAPP_PROVEEDOR=twilio` al teléfono de un integrante (sandbox de Twilio).
5. **Aviso a la cátedra.** El DER cambió respecto de la propuesta entregada: `tipo_actividad.lluvia_max_mm_hora`,
   `evaluacion_climatica.precipitacion_mm` y 5 actividades iniciales en lugar de 3. Los diagramas del
   repositorio ya están actualizados; falta avisar (documento de alcance de la cátedra, sección 9).
6. **Propuesta, sección 01.** Faltan el nombre o número de equipo, la comisión y el responsable de contacto.
7. **Limpieza.** Borrar la rama vacía `persona3-actividades`; `docs/persona4/propuesta-cambios-dominio.md`
   quedó reemplazado por `contratos.md`.

## 5. Cierre (plan de trabajo)

| Ítem | Estado |
|---|---|
| Los diagramas (C1, C2, DER, proceso y estados) coinciden con lo implementado | ⚠️ DER actualizado; revisar C1, C2, proceso y estados antes de la entrega |
| No hay secretos en el repositorio: solo `.env.example` | ✅ solo `.env.example` con valores de ejemplo; el secreto del realm llega por variable |
| Ensayo de la demo: modo SIMULADO con escenario de lluvia y después modo REAL | ⏳ |
| Cada integrante explica su parte a los otros tres | ⏳ |
