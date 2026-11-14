# ClimaFit: parte de la Persona 4

Esta carpeta contiene la documentación de la entrega de Persona 4:

```
docs/persona4/
├── propuesta-cambios-dominio.md   Cambios del chat del 12/11 que requieren acuerdo del equipo
├── README_PERSONA4.md            Límites de integración y detalles técnicos
└── persona4readme.txt             Guía oral de la funcionalidad
```

## 1. Estado de validación

La integración contra el repositorio actual se compiló y probó con Temurin JDK 21 y Maven 3.9.16:

- `ms-clima`: 43 pruebas, sin fallas.
- `ms-clases`: 35 pruebas, sin fallas.
- `climafit-commons`, requerido por ambos módulos: 2 pruebas, sin fallas.

Comando ejecutado desde la raíz:

```powershell
mvn -pl ms-clima,ms-clases -am test
```

La suite no inicia PostgreSQL ni ejecuta Flyway sobre una base real, y no consulta Open-Meteo en vivo. El `V1__esquema_inicial.sql`
actual de `ms-clases` sigue siendo el esqueleto vacío de `main`; para arrancar el servicio y aplicar la V2 hace falta que
Persona 3 agregue primero la V1 real con la tabla `clase`.

## 2. Qué es de la Persona 4 y qué es provisorio

**Código de Persona 4 (se conserva al integrar):**

| Proyecto | Archivos |
|---|---|
| ms-clima | paquete `ar.edu.utn.frc.climafit.clima.*`, perfiles local/demo, propiedades climáticas y pruebas. Se conserva el POM y la aplicación `MsClimaApplication` existentes en el repo; se agregó Caffeine al POM. |
| ms-clases | paquetes `evaluacion`, `reprogramacion`, `tarea`, `client.clima`, `config.ClasesConfig` (solo el `Clock` y `@EnableScheduling`; si la Persona 3 ya los tiene, se descarta), `src/main/resources/db/migration/V2__evaluacion_climatica.sql` y los tests |
| Documentación | `docs/persona4/` |

**PROVISORIO (se borra o se reemplaza por lo real):**

| Archivo | Quién lo reemplaza |
|---|---|
| `clase.Clase`, `clase.ClaseRepository`, `clase.ClaseRespuesta` | Persona 3 |
| `inscripcion.*` | Persona 3 |
| `estado.*` (`EstadoClase`, `CambioEstado`, `CambioEstadoRepository`, `CambioEstadoService`) | Persona 3 |
| `aviso.*` (`AvisoService`, `TipoEvento`, `NotificacionesClient`, `NotificacionesClientLog`) | Persona 3 |
| `client.actividades.*` | Persona 3 (el DTO necesita el campo nuevo `lluviaMaxMmHora`). Solo existe `ActividadesClientStub` (perfil `stub`): sin ese perfil falta el cliente real y ms-clases no arranca. |
| La implementación de apoyo de los paquetes anteriores | Se incluyó en la rama para compilar/probar la parte climática porque `main` todavía no tiene el esqueleto de Persona 3. Al integrar su código, reemplazar esos paquetes sin duplicarlos. |

Se mantuvieron el POM real de `ms-clases`, la aplicación `MsClasesApplication`, la configuración existente y su migración
V1; solo se agregaron las dependencias/propiedades requeridas por Persona 4. El código usa las clases de error y
`UsuarioActual.id()` de `climafit-commons` del repositorio.

Como el cliente HTTP común de commons aún no existe en esta rama base, `ClimaClientHttp` obtiene client credentials
con `OAuth2AuthorizedClientManager`, propaga `X-Correlation-Id` y aplica los timeouts. Cuando Persona 2 publique la
fábrica común prevista en el plan, reemplazar el interceptor local por esa implementación compartida.

## 3. Cómo está pensado (patrones)

* **Strategy, `FuentePronostico`.** Una implementación por modo: `FuenteReal`, `FuenteSimulada`, `FuenteFalla`. El
  `SelectorFuente` guarda el modo activo. El resto del servicio no sabe de dónde salen las horas, y agregar un modo
  nuevo no toca nada existente. `FuenteSimulada` y `FuenteFalla` solo existen con el perfil `demo`.
* **Dominio puro, `EvaluadorClimatico`.** RN10 es una clase sin Spring ni red: recibe horas y criterios, devuelve el
  veredicto con un motivo. Por eso tiene un test por resultado, uno por criterio y uno por cada borde.
* **Puerto y adaptador.** `ProveedorOpenMeteo` (puerto) / `OpenMeteoClient` (adaptador) en ms-clima, y `ClimaClient`
  (puerto) / `ClimaClientHttp` y `ClimaClientStub` en ms-clases. Los tests no llaman a Internet ni a otro servicio.
* **Stub por perfil.** Con `spring.profiles.active=stub` se usan los clientes falsos, configurables desde los tests.
* **Consultar primero, cambiar después.** `EvaluacionService` hace todas las consultas (actividad, espacio, clima, nuevo
  día) antes de cambiar el estado. Si una falla, la clase queda como estaba (RN16). Sin esto, una caída de ms-clima
  justo al buscar el día dejaría la clase SUSPENDIDA, y RN09 solo evalúa PROGRAMADA, REPROGRAMADA y EN_RIESGO: nunca
  se reintentaría sola.
* **Una evaluación por clase en su propia transacción.** `EvaluacionGeneralService` sigue con las demás aunque una falle.

## 4. Dónde está cada regla

| Regla | Código | Tests |
|---|---|---|
| RN05 (mi parte) | `AutorizacionClase` | `AutorizacionClaseTest` (`rn05_*`) |
| RN09 | `EvaluacionService.esEvaluable` + `ClasesAEvaluarRepository` | `EvaluacionServiceTest` (`rn09_*`) |
| RN10 | `EvaluadorClimatico` | `EvaluadorClimaticoTest` (`rn10_*`) |
| RN11 | `EvaluacionService.evaluar` | `EvaluacionServiceTest` (`rn11_*`) |
| RN12 | `DecisionRiesgoService` | `DecisionRiesgoServiceTest` (`rn12_*`) |
| RN13 | `ReprogramacionService` | `ReprogramacionServiceTest` (`rn13_*`) |
| RN16 (mi parte) | `EvaluacionService`, `DecisionRiesgoService`, `FuenteFalla`, `TareaEvaluacionProgramada` | `rn16_*` en ms-clima y ms-clases |

## 5. Decisiones que tomé donde el plan no decía (para confirmar)

1. **Ventana horaria.** Un bloque horario cuenta si se solapa con la clase: una clase de 18:00 a 19:00 usa solo el bloque
   de las 18. (`HorasDeVentana`)
2. **Enteros.** Viento y temperatura se redondean a enteros al leer Open-Meteo, como las columnas `int` del DER.
3. **Motivo.** Si hay varias causas, se listan todas separadas por `;`. En los avisos el motivo va en minúscula inicial.
4. **Ya estaba EN_RIESGO y vuelve EN_RIESGO.** No se cambia el estado (la máquina de estados no permite
   EN_RIESGO a EN_RIESGO) ni se repite la alerta al profesor: la propuesta dice que la clase "sigue vigente".
5. **Responsable.** La suspensión o el riesgo causados por una evaluación llevan el `sub` de quien la pidió (o `SISTEMA`
   en la tarea). La reprogramación automática es siempre `SISTEMA`.
6. **Dos avisos por reprogramación automática:** `SUSPENSION` y después `REPROGRAMACION`, cada uno con su `cambio_estado`.
7. **Evaluación FALLIDA al fallar la búsqueda de nuevo día.** Se guarda una sola evaluación FALLIDA cuyo motivo conserva
   el NO_APTA original, y la clase no cambia.
8. **Decisión de riesgo (RN12).** La puede tomar solo el profesor dueño: ni siquiera un administrador (la propuesta
   dice "Profesor dueño"). El orden de controles es: dueño (403), luego estado (409).
9. **Escenarios simulados.** Gana el último cargado que cubra la hora; la ubicación coincide si difiere menos de
   0,001 grados (unos 110 m); sin escenario, tiempo apto (0 %, 5 km/h, 22 °C, sin tormenta).

## 6. Puntos a acordar con otras personas

* **Persona 3:** `CambioEstadoService.cambiarEstado` no recibe `inicioAnterior` ni `inicioNuevo`, pero la tarea 4.7 pide que
  el `cambio_estado` los guarde. Hoy la Persona 4 los completa con los setters del `CambioEstado` devuelto (dentro de la
  transacción). Lo más limpio sería sobrecargar `cambiarEstado` con esas dos fechas.
* **Persona 3:** `AvisoService.avisar` con lista de destinatarios vacía (clase sin inscriptos): debería marcar
  `notificado = true` sin llamar a ms-notificaciones; si no, `reenviarPendientes` lo reintentaría para siempre.
* **Persona 3:** el plan dice que `AvisoService` arma el mensaje, pero su firma recibe `mensaje`. Los textos los arma
  la Persona 4 (`MensajesClima`). Si prefieren que los arme `AvisoService`, se mueve.
* **Persona 2:** sustituir el interceptor OAuth2 de `ClimaClientHttp` por la fábrica HTTP común cuando esté disponible.
* **Todos:** los cambios de `docs/propuesta-cambios-dominio.md`.

## 7. Correspondencia con la colección de Postman

| Carpeta | Qué ejercita de la Persona 4 | Cubierto en la prueba de punta a punta |
|---|---|---|
| 00 | modo SIMULADO y borrado de escenarios | sí (modo y escenarios) |
| 05 | modo FALLA, evaluación FALLIDA, clase sigue PROGRAMADA | sí (CP6) |
| 06 | lluvia 90 %, NO_APTA, REPROGRAMADA al día siguiente, historial | sí (CP3) |
| 07 | viento 20 km/h, EN_RIESGO, decisión ajena 403, suspender | sí (CP4; el 403 en `AutorizacionClaseTest`) |
| 08 | 8 días de lluvia, SUSPENDIDA_SIN_FECHA | sí (CP5) |
| 09 | techada 409 `CLASE_NO_EVALUABLE`, 409 `CLASE_NO_EN_RIESGO`, evaluación general | sí (RN09, RN12) |
| 10 | modo REAL y pronóstico de Open-Meteo | **no**: requiere Internet |

## 8. Cómo correr

```
# Desde la raíz; -am también compila climafit-commons
mvn -pl ms-clima,ms-clases -am test
```

`ms-clases` requiere PostgreSQL y la migración V1 real de Persona 3 para arrancar. Para probar las rutas protegidas se
requieren tokens válidos de Keycloak; el endpoint interno requiere el rol `SERVICIO`. En Docker Compose, el modo y los
escenarios de ms-clima solo se cargan con el perfil `demo`.
