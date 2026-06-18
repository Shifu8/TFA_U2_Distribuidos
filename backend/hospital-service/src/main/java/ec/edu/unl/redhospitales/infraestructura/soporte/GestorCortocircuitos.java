package ec.edu.unl.redhospitales.infraestructura.soporte;

import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GestorCortocircuitos {

    private final ConcurrentHashMap<Integer, CortocircuitoNodo> circuitos = new ConcurrentHashMap<>();

    private static final int MAX_FALLOS_SEGUIDOS = 2;
    private static final long TIEMPO_ESPERA_ABRIR_MS = 10000;

    public boolean permitirLlamada(int idNodo) {
        return obtenerCircuito(idNodo).permitirLlamada();
    }

    public void registrarExito(int idNodo) {
        obtenerCircuito(idNodo).registrarExito();
    }

    public void registrarFallo(int idNodo) {
        obtenerCircuito(idNodo).registrarFallo();
    }

    private CortocircuitoNodo obtenerCircuito(int idNodo) {
        return circuitos.computeIfAbsent(idNodo, id -> new CortocircuitoNodo(id, MAX_FALLOS_SEGUIDOS, TIEMPO_ESPERA_ABRIR_MS));
    }
}
