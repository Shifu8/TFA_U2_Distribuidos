/*
 * Define consultas de descubrimiento de servicios registradas en Consul.
 */
package ec.edu.unl.redhospitales.dominio.puerto;

import java.util.List;

public interface PuertoDescubrimientoServicios {

    List<String> listarInstanciasHospitalService();
}
