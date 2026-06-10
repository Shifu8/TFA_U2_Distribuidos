/*
 * Recibe desde el frontend la consulta simulada de compatibilidad de donante.
 */
package ec.edu.unl.redhospitales.presentacion.dto;

import ec.edu.unl.redhospitales.dominio.modelo.SolicitudDonante;
import jakarta.validation.constraints.NotBlank;

public record SolicitudDonanteDto(
        @NotBlank String nombreDonante,
        @NotBlank String tipoSangreDonante,
        @NotBlank String nombreReceptor,
        @NotBlank String tipoSangreReceptor,
        String urgencia
) {

    public SolicitudDonante aModelo() {
        return new SolicitudDonante(nombreDonante, tipoSangreDonante, nombreReceptor, tipoSangreReceptor, urgencia);
    }
}
