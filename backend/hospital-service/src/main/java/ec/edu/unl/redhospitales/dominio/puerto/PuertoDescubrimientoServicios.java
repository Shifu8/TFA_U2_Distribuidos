/*
 * Define consultas de descubrimiento de servicios registradas en Consul.
 */
package ec.edu.unl.redhospitales.dominio.puerto;

import java.util.List;
import java.util.Set;

public interface PuertoDescubrimientoServicios {

    List<String> listarInstanciasHospitalService();
    Set<Integer> obtenerNodosActivosEnConsul();
}
