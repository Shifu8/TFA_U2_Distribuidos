/*
 * Representa un evento de log entregado por la API REST.
 */
package ec.edu.unl.redhospitales.presentacion.dto;

import ec.edu.unl.redhospitales.dominio.modelo.EventoSistema;

import java.time.Instant;

public record EventoSistemaDto(
        Instant fecha,
        int nodoId,
        String categoria,
        String descripcion
) {

    public static EventoSistemaDto desde(EventoSistema evento) {
        return new EventoSistemaDto(evento.getFecha(), evento.getNodoId(), evento.getCategoria(), evento.getDescripcion());
    }
}
