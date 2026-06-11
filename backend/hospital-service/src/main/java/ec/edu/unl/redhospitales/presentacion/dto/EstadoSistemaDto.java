/*
 * Resumen general del nodo consultado y los algoritmos distribuidos.
 */
package ec.edu.unl.redhospitales.presentacion.dto;

import ec.edu.unl.redhospitales.dominio.modelo.EstadoCristian;
import ec.edu.unl.redhospitales.dominio.modelo.EstadoExclusionMutua;

import java.util.List;

public record EstadoSistemaDto(
        NodoHospitalarioDto nodoLocal,
        NodoHospitalarioDto coordinador,
        long nodosActivos,
        List<NodoHospitalarioDto> nodos,
        EstadoCristian cristian,
        EstadoExclusionMutua exclusion
) {
}
