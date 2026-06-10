/*
 * Entrega el resultado de compatibilidad calculado por el servicio de aplicacion.
 */
package ec.edu.unl.redhospitales.presentacion.dto;

import ec.edu.unl.redhospitales.dominio.modelo.ResultadoCompatibilidad;

public record ResultadoCompatibilidadDto(
        boolean compatible,
        String mensaje,
        String coordinadorConsultado,
        String urgencia
) {

    public static ResultadoCompatibilidadDto desde(ResultadoCompatibilidad resultado) {
        return new ResultadoCompatibilidadDto(
                resultado.isCompatible(),
                resultado.getMensaje(),
                resultado.getCoordinadorConsultado(),
                resultado.getUrgencia()
        );
    }
}
