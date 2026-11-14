package ar.edu.utn.frc.climafit.actividades;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ms-actividades: catálogo del dominio. Tipos de actividad con sus criterios climáticos,
 * espacios con coordenadas y ejercicios (base y personalizados).
 * La seguridad, el formato de errores y la trazabilidad llegan con climafit-commons.
 */
@SpringBootApplication
public class MsActividadesApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsActividadesApplication.class, args);
    }
}
