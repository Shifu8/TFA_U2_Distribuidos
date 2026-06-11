/*
 * Resume la ultima sincronizacion de reloj realizada con el algoritmo de Cristian.
 */
package ec.edu.unl.redhospitales.dominio.modelo;

import java.time.Instant;

public class EstadoCristian {

    private Instant horaLocal;
    private Instant horaCoordinador;
    private Instant horaAjustada;
    private long rttMs;
    private long latenciaMs;
    private boolean ajusteSistemaHabilitado;
    private boolean relojSistemaAjustado;

    public EstadoCristian() {
    }

    public EstadoCristian(Instant horaLocal, Instant horaCoordinador, Instant horaAjustada, long rttMs, long latenciaMs) {
        this(horaLocal, horaCoordinador, horaAjustada, rttMs, latenciaMs, false, false);
    }

    public EstadoCristian(
            Instant horaLocal,
            Instant horaCoordinador,
            Instant horaAjustada,
            long rttMs,
            long latenciaMs,
            boolean ajusteSistemaHabilitado,
            boolean relojSistemaAjustado
    ) {
        this.horaLocal = horaLocal;
        this.horaCoordinador = horaCoordinador;
        this.horaAjustada = horaAjustada;
        this.rttMs = rttMs;
        this.latenciaMs = latenciaMs;
        this.ajusteSistemaHabilitado = ajusteSistemaHabilitado;
        this.relojSistemaAjustado = relojSistemaAjustado;
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

    public Instant getHoraAjustada() {
        return horaAjustada;
    }

    public void setHoraAjustada(Instant horaAjustada) {
        this.horaAjustada = horaAjustada;
    }

    public long getRttMs() {
        return rttMs;
    }

    public void setRttMs(long rttMs) {
        this.rttMs = rttMs;
    }

    public long getLatenciaMs() {
        return latenciaMs;
    }

    public void setLatenciaMs(long latenciaMs) {
        this.latenciaMs = latenciaMs;
    }

    public boolean isAjusteSistemaHabilitado() {
        return ajusteSistemaHabilitado;
    }

    public void setAjusteSistemaHabilitado(boolean ajusteSistemaHabilitado) {
        this.ajusteSistemaHabilitado = ajusteSistemaHabilitado;
    }

    public boolean isRelojSistemaAjustado() {
        return relojSistemaAjustado;
    }

    public void setRelojSistemaAjustado(boolean relojSistemaAjustado) {
        this.relojSistemaAjustado = relojSistemaAjustado;
    }
}
