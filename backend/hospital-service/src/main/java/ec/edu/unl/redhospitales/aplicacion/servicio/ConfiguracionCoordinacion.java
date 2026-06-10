/*
 * Agrupa tiempos y limites usados por heartbeat, Bully, Cristian y logs.
 */
package ec.edu.unl.redhospitales.aplicacion.servicio;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "coordinacion")
public class ConfiguracionCoordinacion {

    private long heartbeatMs = 2000;
    private long timeoutHeartbeatMs = 5000;
    private long esperaRespuestaEleccionMs = 1800;
    private long sincronizacionCristianMs = 8000;
    private int maximoLogs = 300;

    public long getHeartbeatMs() {
        return heartbeatMs;
    }

    public void setHeartbeatMs(long heartbeatMs) {
        this.heartbeatMs = heartbeatMs;
    }

    public long getTimeoutHeartbeatMs() {
        return timeoutHeartbeatMs;
    }

    public void setTimeoutHeartbeatMs(long timeoutHeartbeatMs) {
        this.timeoutHeartbeatMs = timeoutHeartbeatMs;
    }

    public long getEsperaRespuestaEleccionMs() {
        return esperaRespuestaEleccionMs;
    }

    public void setEsperaRespuestaEleccionMs(long esperaRespuestaEleccionMs) {
        this.esperaRespuestaEleccionMs = esperaRespuestaEleccionMs;
    }

    public long getSincronizacionCristianMs() {
        return sincronizacionCristianMs;
    }

    public void setSincronizacionCristianMs(long sincronizacionCristianMs) {
        this.sincronizacionCristianMs = sincronizacionCristianMs;
    }

    public int getMaximoLogs() {
        return maximoLogs;
    }

    public void setMaximoLogs(int maximoLogs) {
        this.maximoLogs = maximoLogs;
    }
}
