/*
 * Define como cargar la lista de nodos hospitalarios configurados.
 */
package ec.edu.unl.redhospitales.dominio.puerto;

import ec.edu.unl.redhospitales.dominio.modelo.NodoHospitalario;

import java.util.List;

public interface PuertoConfiguracionNodos {

    List<NodoHospitalario> cargarNodos();
}
