/*
 * Resume una sincronizacion de tiempo realizada con el algoritmo de Cristian.
 */
package ec.edu.unl.redhospitales.dominio.modelo;

import java.time.Instant;

public class ResultadoCristian {

    private Instant horaLocal;
    private Instant horaCoordinador;
    private long latenciaEstimadaMs;
    private Instant horaAjustada;

    public ResultadoCristian() {
    }

    public ResultadoCristian(Instant horaLocal, Instant horaCoordinador, long latenciaEstimadaMs, Instant horaAjustada) {
        this.horaLocal = horaLocal;
        this.horaCoordinador = horaCoordinador;
        this.latenciaEstimadaMs = latenciaEstimadaMs;
        this.horaAjustada = horaAjustada;
    }

    public Instant getHoraLocal() {
        return horaLocal;
    }

    public void setHoraLocal(Instant horaLocal) {
        this.horaLocal = horaLocal;
    }

    public Instant getHoraCoordinador() {
        return horaCoordinador;
    }

    public void setHoraCoordinador(Instant horaCoordinador) {
        this.horaCoordinador = horaCoordinador;
    }

    public long getLatenciaEstimadaMs() {
        return latenciaEstimadaMs;
    }

    public void setLatenciaEstimadaMs(long latenciaEstimadaMs) {
        this.latenciaEstimadaMs = latenciaEstimadaMs;
    }

    public Instant getHoraAjustada() {
        return horaAjustada;
    }

    public void setHoraAjustada(Instant horaAjustada) {
        this.horaAjustada = horaAjustada;
    }
}
