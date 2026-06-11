/*
 * Estado visible de la exclusion mutua por servidor central.
 */
package ec.edu.unl.redhospitales.dominio.modelo;

import java.util.List;

public record EstadoExclusionMutua(
        int nodoLocalId,
        int idCoordinador,
        boolean coordinadorLocal,
        boolean seccionCriticaOcupada,
        Integer nodoEnSeccionCritica,
        List<Integer> colaEspera,
        boolean solicitudLocalPendiente,
        boolean accesoLocalConcedido,
        String recurso,
        String estadoLocal
) {
}
