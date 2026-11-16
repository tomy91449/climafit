# ms-clases — parte de la Persona 3

Gestión de clases, inscripciones, estados, historial y avisos. Puerto 8082, base `clases_db`.
Mis paquetes: `clase`, `inscripcion`, `estado`, `historial`, `aviso`, `client.actividades`.
Paquetes de la Persona 4 (no tocar): `evaluacion`, `reprogramacion`, `tarea`, `client.clima`.

## Cómo correrlo

```bash
# 1) Instalar climafit-commons en el repositorio local de Maven:
mvn -f ../climafit-commons/pom.xml install
# 2) Tests (usan H2 en modo PostgreSQL y los clientes falsos, no necesitan nada más):
mvn test
# 3) Arrancar sin ms-actividades ni ms-notificaciones (clientes falsos):
mvn spring-boot:run -Dspring-boot.run.profiles=stub
```

## Cómo está armado (y por qué)

| Pieza | Qué hace | Por qué así |
|---|---|---|
| `EstadoClase` | Tabla de "desde este estado solo se puede ir a estos". | Una máquina de estados evita estados imposibles (una clase `CANCELADA` nunca vuelve). |
| `CambioEstadoService.cambiarEstado(...)` | Único lugar que cambia el estado: valida, guarda `estado_previo` y escribe `cambio_estado`. | Si todos pasan por acá, el historial nunca se desincroniza con el estado real. |
| `ClaseRepository.existeSuperposicionProfesor(...)` | RN04. Firma fija de la Fase 0. | Dos clases se pisan si una empieza antes de que termine la otra. |
| `InscripcionService.inscribir` | RN01 a RN03 con `SELECT ... FOR UPDATE` sobre la clase. | Si dos alumnos van por el último lugar, el segundo espera y recién entonces cuenta: el cupo no se pasa. |
| `AutorizacionClase` | RN05: compara el `sub` del token con `profesor_id`. | Lo reutiliza la Persona 4 en sus endpoints. |
| `AvisoService` | Arma `CLASE-{claseId}-CAMBIO-{cambioId}`, manda el aviso y marca `notificado`. | RN16: si ms-notificaciones falla, el estado **no** se deshace; queda `notificado = false` y `reenviarPendientes()` lo reintenta con el mismo `eventoId` (RN15 evita duplicados). |
| Clientes con perfil `stub` / real | Interfaz + implementación falsa + implementación HTTP. | Permite avanzar y testear sin que los otros servicios existan. |

Los avisos se mandan **después** de confirmar la transacción: una llamada HTTP lenta no retiene el bloqueo de la fila, y si falla el cambio ya quedó guardado.

## Reglas y dónde están

| Regla | Código | Tests |
|---|---|---|
| RN01 cupo | `InscripcionService.inscribir` | `rn01_*` |
| RN02 clase disponible / no repetida | `InscripcionService` | `rn02_*` |
| RN03 superposición del alumno | `ClaseRepository.existeSuperposicionAlumno` | `rn03_*` |
| RN04 superposición del profesor | `ClaseRepository.existeSuperposicionProfesor` | `rn04_*` |
| RN05 autorización | `AutorizacionClase` | `rn05_*` |
| RN06 clase cancelada o ya empezada | `CambioEstadoService.validarModificable` | `rn06_*` |
| RN07 rutina compatible (la valido yo, la regla es de la P2) | `ClaseService.verificarRutina` | `rn07_*` |
| RN16 avisos pendientes | `AvisoService` | `rn16_*` |

## Integración con climafit-commons

- `ActividadesClientReal` y `NotificacionesClientReal` usan `ClienteServicioFactory`, que agrega el token de servicio, los timeouts (2 s / 5 s), los reintentos de lecturas y el correlation id. Cualquier falla de red o timeout se traduce en 503 `SERVICIO_NO_DISPONIBLE`.
- `ClimaClientHttp` arma su propio cliente porque necesita esperar más (15 s): ms-clima le da hasta 10 s a Open-Meteo.
- El `OAuth2AuthorizedClientManager` del token de servicio lo crea commons (`ClimafitHttpAutoConfiguration`).

## Para charlar con el equipo (nada se inventa)

1. `ActividadesClient`: la Fase 0 solo lista `obtenerActividad` y `obtenerEspacio`, pero la tarea 3.3 pide también `verificarCompatibilidad`. Lo dejé así; confirmar y anotar en `docs/contratos.md`.
2. RN06 y clases `SUSPENDIDA` / `SUSPENDIDA_SIN_FECHA`: su inicio viejo ya pasó y, si bloqueara, nunca se podrían reagendar (CP5). Las exceptué. Confirmar con la Persona 4.
3. `cambiarEstado` tiene una **sobrecarga con `nuevoInicio`** para reprogramaciones (guarda `inicio_anterior` / `inicio_nuevo`). La firma de 4 parámetros de la Fase 0 no cambió.
4. Firmas fijas que no se tocan sin acuerdo de los 4: `cambiarEstado`, `avisar`, `existeSuperposicionProfesor`, `findByClaseIdAndEstado`.
