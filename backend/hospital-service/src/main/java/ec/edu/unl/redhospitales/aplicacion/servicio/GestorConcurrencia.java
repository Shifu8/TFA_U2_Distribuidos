/*
 * Singleton que administra la cola FIFO de la exclusion mutua centralizada.
 */
package ec.edu.unl.redhospitales.aplicacion.servicio;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public final class GestorConcurrencia {

    private static final GestorConcurrencia INSTANCIA = new GestorConcurrencia();

    private final Queue<Integer> colaFifo = new ArrayDeque<>();
    private Integer nodoEnSeccionCritica;

    private GestorConcurrencia() {
    }

    public static GestorConcurrencia obtenerInstancia() {
        return INSTANCIA;
    }

    public synchronized ResultadoSolicitud solicitarAcceso(int nodoId) {
        if (nodoEnSeccionCritica == null && colaFifo.isEmpty()) {
            nodoEnSeccionCritica = nodoId;
            return new ResultadoSolicitud(true, nodoEnSeccionCritica, snapshotCola());
        }

        if (nodoEnSeccionCritica != null && nodoEnSeccionCritica == nodoId) {
            return new ResultadoSolicitud(true, nodoEnSeccionCritica, snapshotCola());
        }

        if (!colaFifo.contains(nodoId)) {
            colaFifo.offer(nodoId);
        }
        return new ResultadoSolicitud(false, nodoEnSeccionCritica, snapshotCola());
    }

    public synchronized ResultadoLiberacion liberarAcceso(int nodoId) {
        boolean liberado = false;
        if (nodoEnSeccionCritica != null && nodoEnSeccionCritica == nodoId) {
            liberado = true;
            nodoEnSeccionCritica = colaFifo.poll();
        } else {
            colaFifo.remove(nodoId);
        }
        return new ResultadoLiberacion(liberado, nodoEnSeccionCritica, snapshotCola());
    }

    public synchronized void reiniciar() {
        colaFifo.clear();
        nodoEnSeccionCritica = null;
    }

    public synchronized Integer getNodoEnSeccionCritica() {
        return nodoEnSeccionCritica;
    }

    public synchronized List<Integer> snapshotCola() {
        return new ArrayList<>(colaFifo);
    }

    public record ResultadoSolicitud(boolean concedido, Integer nodoEnSeccionCritica, List<Integer> colaEspera) {
    }

    public record ResultadoLiberacion(boolean liberado, Integer siguienteNodo, List<Integer> colaEspera) {
    }
}
