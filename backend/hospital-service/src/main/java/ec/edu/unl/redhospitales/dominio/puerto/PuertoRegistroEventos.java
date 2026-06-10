/*
 * Define el almacenamiento y consulta de eventos del sistema.
 */
package ec.edu.unl.redhospitales.dominio.puerto;

import ec.edu.unl.redhospitales.dominio.modelo.EventoSistema;

import java.util.List;

public interface PuertoRegistroEventos {

    void registrar(EventoSistema evento);

    List<EventoSistema> listar();
}
