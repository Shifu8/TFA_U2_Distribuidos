/*
 * Enumera los tipos de mensajes TCP intercambiados entre hospitales.
 */
package ec.edu.unl.redhospitales.dominio.enumeracion;

public enum TipoMensaje {
    HEARTBEAT,
    ELECTION,
    ANSWER,
    COORDINATOR,
    SYNC_TIME_REQUEST,
    SYNC_TIME_RESPONSE,
    DONOR_REQUEST,
    DONOR_RESPONSE
}
