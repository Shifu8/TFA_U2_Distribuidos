/*
 * Expone datos de un nodo hospitalario al frontend sin revelar detalles internos.
 */
package ec.edu.unl.redhospitales.presentacion.dto;

import ec.edu.unl.redhospitales.dominio.modelo.NodoHospitalario;

import java.time.Instant;

public record NodoHospitalarioDto(
        int id,
        String nombreHospital,
        String host,
        int tcpPort,
        int httpPort,
        String estado,
        String rol,
        Instant ultimaSenal,
        long relojLogicoLamport
) {

    public static NodoHospitalarioDto desde(NodoHospitalario nodo) {
        return new NodoHospitalarioDto(
                nodo.getId(),
                nodo.getNombreHospital(),
                nodo.getHost(),
                nodo.getTcpPort(),
                nodo.getHttpPort(),
                nodo.getEstado().name(),
                nodo.getRol().name(),
                nodo.getUltimaSenal(),
                nodo.getRelojLogicoLamport()
        );
    }
}
