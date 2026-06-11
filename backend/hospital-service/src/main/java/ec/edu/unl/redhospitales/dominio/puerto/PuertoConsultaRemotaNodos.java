/*
 * Define consultas HTTP internas para leer el estado y los eventos locales de otros nodos.
 */
package ec.edu.unl.redhospitales.dominio.puerto;

import ec.edu.unl.redhospitales.dominio.modelo.EventoSistema;
import ec.edu.unl.redhospitales.dominio.modelo.EstadoExclusionMutua;
import ec.edu.unl.redhospitales.dominio.modelo.NodoHospitalario;

import java.util.List;
import java.util.Optional;

public interface PuertoConsultaRemotaNodos {

    Optional<NodoHospitalario> consultarEstadoLocal(NodoHospitalario nodo);

    List<EventoSistema> consultarEventosLocales(NodoHospitalario nodo);

    Optional<EstadoExclusionMutua> consultarEstadoExclusionLocal(NodoHospitalario nodo);
}
