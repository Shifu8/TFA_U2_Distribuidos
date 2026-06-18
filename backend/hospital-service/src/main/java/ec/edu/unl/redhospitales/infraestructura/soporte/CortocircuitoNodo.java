package ec.edu.unl.redhospitales.infraestructura.soporte;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CortocircuitoNodo {

    private static final Logger LOGGER = LoggerFactory.getLogger(CortocircuitoNodo.class);

    public enum Estado {
        CERRADO,
        ABIERTO
    }

    private final int idNodo;
    private final int maxFallosSeguidos;
    private final long tiempoEsperaAbrirMs;

    private Estado estado = Estado.CERRADO;
    private int fallosSeguidos = 0;
    private long tiempoAperturaMs = 0;

    public CortocircuitoNodo(int idNodo, int maxFallosSeguidos, long tiempoEsperaAbrirMs) {
        this.idNodo = idNodo;
        this.maxFallosSeguidos = maxFallosSeguidos;
        this.tiempoEsperaAbrirMs = tiempoEsperaAbrirMs;
    }

    public synchronized boolean permitirLlamada() {
        if (estado == Estado.ABIERTO) {
            long transcurrido = System.currentTimeMillis() - tiempoAperturaMs;
            if (transcurrido > tiempoEsperaAbrirMs) {
                LOGGER.info("[CIRCUIT BREAKER] Reintentando llamada de prueba al nodo {} (Fin de tiempo de espera)", idNodo);
                return true;
            }
            return false;
        }
        return true;
    }

    public synchronized void registrarExito() {
        if (estado != Estado.CERRADO || fallosSeguidos > 0) {
            LOGGER.info("[CIRCUIT BREAKER] Canal al nodo {} restablecido exitosamente a CERRADO", idNodo);
        }
        fallosSeguidos = 0;
        estado = Estado.CERRADO;
    }

    public synchronized void registrarFallo() {
        fallosSeguidos++;
        if (estado == Estado.CERRADO && fallosSeguidos >= maxFallosSeguidos) {
            estado = Estado.ABIERTO;
            tiempoAperturaMs = System.currentTimeMillis();
            LOGGER.warn("[CIRCUIT BREAKER] Circuito ABIERTO para el nodo {} debido a {} fallos consecutivos. Evitando llamadas por {} ms.",
                    idNodo, fallosSeguidos, tiempoEsperaAbrirMs);
        } else if (estado == Estado.ABIERTO) {
            tiempoAperturaMs = System.currentTimeMillis();
            LOGGER.warn("[CIRCUIT BREAKER] Llamada de prueba al nodo {} falló. El circuito continúa ABIERTO por otros {} ms.",
                    idNodo, tiempoEsperaAbrirMs);
        }
    }

    public Estado getEstado() {
        return estado;
    }

    public int getFallosSeguidos() {
        return fallosSeguidos;
    }
}
