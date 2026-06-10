/*
 * Estandariza respuestas simples para acciones de simulacion y eleccion.
 */
package ec.edu.unl.redhospitales.presentacion.dto;

public record RespuestaOperacionDto(
        boolean correcto,
        String mensaje
) {
}
