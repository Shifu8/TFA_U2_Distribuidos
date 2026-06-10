/*
 * Guarda logs recientes en memoria para consultarlos desde la API.
 */
package ec.edu.unl.redhospitales.infraestructura.persistencia;

import ec.edu.unl.redhospitales.aplicacion.servicio.ConfiguracionCoordinacion;
import ec.edu.unl.redhospitales.dominio.modelo.EventoSistema;
import ec.edu.unl.redhospitales.dominio.puerto.PuertoRegistroEventos;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class RepositorioEventosMemoria implements PuertoRegistroEventos {

    private final ArrayDeque<EventoSistema> eventos = new ArrayDeque<>();
    private final ConfiguracionCoordinacion configuracionCoordinacion;

    public RepositorioEventosMemoria(ConfiguracionCoordinacion configuracionCoordinacion) {
        this.configuracionCoordinacion = configuracionCoordinacion;
    }

    @Override
    public synchronized void registrar(EventoSistema evento) {
        eventos.addLast(evento);
        while (eventos.size() > configuracionCoordinacion.getMaximoLogs()) {
            eventos.removeFirst();
        }
    }

    @Override
    public synchronized List<EventoSistema> listar() {
        return new ArrayList<>(eventos).stream()
                .sorted(Comparator.comparing(EventoSistema::getFecha).reversed())
                .toList();
    }
}
