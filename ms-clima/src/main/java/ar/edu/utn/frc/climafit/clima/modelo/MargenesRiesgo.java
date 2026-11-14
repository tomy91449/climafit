package ar.edu.utn.frc.climafit.clima.modelo;

/**
 * Cercanía a un límite a partir de la cual el resultado pasa de APTA a EN_RIESGO.
 * Los tres primeros son los de la propuesta (RN10). El cuarto es el margen para los mm de
 * lluvia por hora, que la propuesta no tiene: es una decisión provisoria (cambio de dominio).
 */
public record MargenesRiesgo(int lluviaPuntos, int vientoKmh, int temperaturaC,
                             double lluviaMmProporcion) {

    public static final MargenesRiesgo POR_DEFECTO = new MargenesRiesgo(10, 10, 2, 0.20);
}
