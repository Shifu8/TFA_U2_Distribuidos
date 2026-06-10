/*
 * Define el punto de entrada para mensajes TCP recibidos desde infraestructura.
 */
package ec.edu.unl.redhospitales.dominio.puerto;

import ec.edu.unl.redhospitales.dominio.modelo.MensajeTcp;

public interface PuertoManejadorMensajesTcp {

    void manejar(MensajeTcp mensaje);
}
