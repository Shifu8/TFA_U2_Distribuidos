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
    MUTEX_REQUEST,
    MUTEX_GRANT,
    MUTEX_RELEASE,
    DONOR_REQUEST,
    DONOR_RESPONSE
}
