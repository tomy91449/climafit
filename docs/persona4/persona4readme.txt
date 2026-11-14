CLIMAFIT - GUIA ORAL DE LA PERSONA 4
====================================

OBJETIVO DE MI PARTE
--------------------
La Persona 4 implementa ms-clima y la parte climática de ms-clases. El sistema consulta
pronósticos horarios, evalúa si una actividad al aire libre es apta, avisa al profesor si
hay riesgo y, si las condiciones no son aptas, suspende y busca automáticamente una fecha
alternativa.

La implementación sigue las tareas 4.1 a 4.9 del plan. El código de ms-clases incluye
archivos provisorios de Personas 2 y 3 para permitir el desarrollo aislado; esos archivos
no reemplazan los módulos definitivos del equipo.


1. RECORRIDO DE UNA EVALUACIÓN
------------------------------
1. Un profesor evalúa una clase o la tarea programada selecciona las clases al aire libre
   que comienzan dentro de las próximas 24 horas (RN09).
2. ms-clases consulta a ms-actividades los criterios de la actividad y las coordenadas del
   espacio.
3. ms-clases envía a ms-clima una o más ventanas horarias. Para buscar una nueva fecha,
   agrupa los días candidatos en una única llamada.
4. ms-clima obtiene las horas pronosticadas y EvaluadorClimatico aplica RN10 usando el peor
   valor de cada ventana: máxima probabilidad de lluvia, máximo viento, mínima temperatura,
   máxima precipitación en mm/h y presencia de tormenta.
5. Si el resultado es APTA, no se cambia el estado. Si es EN_RIESGO, la clase pasa a ese
   estado y se notifica al profesor. Si es NO_APTA, se suspende, se avisa a los inscriptos
   y se busca una fecha viable dentro de los siete días siguientes (RN11 y RN13).
6. La primera fecha APTA sin superposición de clases del profesor se guarda como
   REPROGRAMADA. Si no hay fecha apta, el estado queda SUSPENDIDA_SIN_FECHA.
7. Se guarda el resultado y el historial de estados. Si falla un servicio externo, la
   evaluación queda FALLIDA y no se cambia el estado que debe poder reintentarse (RN16).

En una clase EN_RIESGO, el profesor puede elegir MANTENER (se vuelve al estado previo) o
SUSPENDER. La decisión está limitada al profesor dueño según RN05 y RN12.


2. COMPONENTES Y TECNOLOGÍAS
----------------------------

JAVA 21 Y SPRING BOOT
Java 21 es la versión indicada en el plan. Spring Boot organiza cada microservicio, crea
los componentes y expone las APIs REST. RestClient se usa para consultar Open-Meteo y para
la comunicación HTTP entre ms-clases y ms-clima, evitando agregar SDKs externos.

JPA / SPRING DATA JPA EN ms-clases
La entidad EvaluacionClimatica representa una fila de evaluacion_climatica. La interfaz
EvaluacionClimaticaRepository extiende JpaRepository para guardar y consultar evaluaciones
sin escribir SQL repetitivo para las operaciones comunes. La migración Flyway V2 crea la
tabla y el índice de consulta por clase y fecha.

JPA se usa en ms-clases porque las evaluaciones son parte del historial persistente del
dominio y deben sobrevivir al reinicio del servicio. También se usa una transacción para
mantener consistentes la clase, la evaluación y sus cambios de estado. La V1 y otras
entidades de clases pertenecen a la Persona 3: los archivos correspondientes en este ZIP
son provisorios.

CAFFEINE EN ms-clima
FuenteReal utiliza una caché Caffeine en memoria. La clave combina latitud, longitud y día;
cada entrada vence a los 30 minutos. Si una consulta necesita varios días, solo solicita
los días que faltan en caché.

El pronóstico es información temporal y puede volver a consultarse; no hace falta guardarlo
como historial permanente. Por eso se usa una caché rápida y local en ms-clima en lugar de
persistirlo con JPA. Esto reduce llamadas a Open-Meteo y evita agregar una base de datos a
un servicio que no necesita almacenamiento duradero.

PATRONES UTILIZADOS
- Strategy: FuentePronostico tiene fuentes REAL, SIMULADO y FALLA; SelectorFuente elige la
  fuente activa sin que la lógica RN10 dependa de su implementación.
- Puerto/adaptador: ProveedorOpenMeteo abstrae el acceso al pronóstico y OpenMeteoClient
  hace la llamada HTTP; ClimaClient abstrae la comunicación desde ms-clases.
- Dominio separado de infraestructura: EvaluadorClimatico implementa RN10 como lógica Java
  pura, que puede probarse sin levantar Spring, una base o Internet.
- Reloj inyectable: Clock permite fijar fechas en pruebas y usar la zona horaria de Córdoba
  en el servicio.


3. REGLAS Y CLASES PARA EXPLICAR
--------------------------------
ms-clima:
- OpenMeteoClient consulta datos horarios de temperatura, probabilidad y mm de
  precipitación, viento y código meteorológico en America/Argentina/Cordoba.
- FuenteReal aplica la caché Caffeine de 30 minutos.
- FuenteSimulada permite probar escenarios deterministas sin depender de Internet. Si no
  hay escenario, usa el tiempo apto predeterminado acordado para la demo.
- FuenteFalla permite comprobar el manejo de errores 503.
- EvaluadorClimatico aplica RN10. Superar cualquiera de los límites genera NO_APTA; estar
  dentro de los márgenes definidos genera EN_RIESGO; en otro caso genera APTA.

ms-clases:
- EvaluacionClimatica y EvaluacionClimaticaRepository persisten y consultan los resultados.
- EvaluacionService coordina consultas, guarda el resultado y aplica RN11. Consulta antes
  de cambiar el estado para que una caída no deje la clase a medio procesar.
- ReprogramacionService prueba los siete días siguientes, descarta superposiciones del
  profesor y escoge la primera evaluación APTA.
- DecisionRiesgoService aplica RN12. Si una dependencia falla al suspender, registra una
  evaluación FALLIDA, propaga el error de servicio y conserva EN_RIESGO para permitir otro
  intento.
- EvaluacionGeneralService evalúa las clases elegibles individualmente.
- TareaEvaluacionProgramada ejecuta cada tres horas la evaluación general y primero invoca
  el reenvío de avisos pendientes de la Persona 3.

Las decisiones de RN10 a RN13, RN16 y las excepciones de borde tienen pruebas unitarias.
Los nombres de las pruebas incluyen el identificador de regla, por ejemplo rn10_*, rn12_*.


4. ENDPOINTS DE PERSONA 4
-------------------------
ms-clima:
GET    /clima/pronostico
POST   /internal/evaluaciones
PUT    /clima/modo                 (solo perfil demo)
POST   /clima/escenarios           (solo perfil demo)
DELETE /clima/escenarios           (solo perfil demo)

ms-clases:
POST  /clases/{id}/evaluaciones
GET   /clases/{id}/evaluaciones
POST  /clases/evaluaciones
PATCH /clases/{id}/decision-riesgo

El endpoint interno requiere el rol SERVICIO. El gateway del equipo no debe exponer
/internal/**. Los permisos de usuario se terminan de integrar con climafit-commons y la
configuración de seguridad del equipo.


5. CAMBIOS DEL DOMINIO COMPARTIDOS
----------------------------------
El chat del 12/11 agrega lluvia máxima en mm/h, acrobacia aérea, patinaje y criterios más
estrictos para deportes. Esta entrega implementa en la parte de Persona 4 los campos
opcionales lluviaMaxMmHora (criterio) y precipitacionMm (pronóstico y evaluación). Si el
criterio no se envía, el sistema conserva compatibilidad con el contrato anterior.

Los umbrales por actividad y la columna correspondiente en ms-actividades son responsabilidad
de Persona 2. El DTO usado por ms-clases necesita coincidir con la implementación de Persona
3. Antes del merge, las cuatro personas deben acordar los contratos, valores y diagramas;
el documento docs/propuesta-cambios-dominio.md enumera la propuesta de Persona 4.


6. PREPARACIÓN PARA INTEGRACIÓN
-------------------------------
Conservar los paquetes evaluacion, reprogramacion, tarea y client.clima de ms-clases, además
de la configuración de Clock y scheduling si Persona 3 no tiene ya una equivalente.
Conservar la migración V2 de evaluacion_climatica, después de comprobar que la V1 real crea
la tabla clase esperada.

Reemplazar por los módulos reales de Personas 2 y 3:
- los paquetes commons.error, commons.seguridad y provisorio;
- las clases provisionales clase, estado, inscripcion, aviso y client.actividades;
- el pom de ms-clases y la migración provisional V1 que está bajo docs/provisorio.

ClimaClientHttp usa OAuth2AuthorizedClientManager para pedir el token client-credentials al
cliente climafit-servicios, agrega Bearer a la llamada y propaga X-Correlation-Id. En el
repositorio actual commons todavía no implementa la fábrica HTTP mencionada en la propuesta;
cuando esté disponible, reemplazar este interceptor local por esa fábrica compartida.


7. CÓMO EJECUTAR Y ESTADO DE VALIDACIÓN
---------------------------------------
Desde cada carpeta ms-clima y ms-clases:
    mvn test

ms-clima puede iniciarse localmente con el perfil demo para probar SIMULADO, FALLA y los
escenarios. ms-clases requiere que la base PostgreSQL y la migración V1 real estén disponibles
para iniciar la aplicación completa.

Se instaló Temurin JDK 21 y Maven 3.9.16 para ejecutar las pruebas: ms-clima pasó 43 pruebas
y ms-clases pasó 35 pruebas. La suite comprueba la lógica y compila el código, pero no
levanta una base PostgreSQL real ni ejecuta las migraciones contra esa base. Tampoco se
consultó Open-Meteo en vivo; esos puntos y la integración del token de servicio deben
verificarse antes del merge.
