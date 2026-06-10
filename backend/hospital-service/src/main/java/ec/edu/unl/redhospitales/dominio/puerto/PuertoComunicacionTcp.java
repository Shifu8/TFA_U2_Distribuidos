/*
 * Define el envio de mensajes distribuidos mediante un adaptador TCP.
 */
package ec.edu.unl.redhospitales.dominio.puerto;

import ec.edu.unl.redhospitales.dominio.modelo.MensajeTcp;
import ec.edu.unl.redhospitales.dominio.modelo.NodoHospitalario;

public interface PuertoComunicacionTcp {

    boolean enviar(MensajeTcp mensaje, NodoHospitalario destino);
}
