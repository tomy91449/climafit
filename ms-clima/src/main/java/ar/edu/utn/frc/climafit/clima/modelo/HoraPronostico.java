package ar.edu.utn.frc.climafit.clima.modelo;

import java.time.LocalDateTime;

/**
 * Pronóstico de una hora. Viento y temperatura se guardan como enteros (se redondean al
 * convertir desde Open-Meteo) para que coincidan con las columnas int del DER de
 * evaluacion_climatica y para que lo que se compara sea lo mismo que lo que se informa.
 *
 * @param fechaHora       inicio del bloque horario (hora de Córdoba)
 * @param probLluvia      probabilidad de lluvia, 0 a 100
 * @param precipitacionMm milímetros de agua previstos en esa hora (cambio de dominio del 12/11)
 */
public record HoraPronostico(LocalDateTime fechaHora, int probLluvia, int vientoKmh,
                             int temperaturaC, boolean tormenta, double precipitacionMm) {
}
