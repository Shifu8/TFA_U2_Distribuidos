/*
 * Transporta eventos distribuidos entre nodos mediante sockets TCP.
 */
package ec.edu.unl.redhospitales.dominio.modelo;

import ec.edu.unl.redhospitales.dominio.enumeracion.TipoMensaje;

import java.time.Instant;

public class MensajeTcp {

    private TipoMensaje tipo;
    private int idNodoOrigen;
    private int idNodoDestino;
    private String contenido;
    private Instant marcaTiempo;

    public MensajeTcp() {
    }

    public MensajeTcp(TipoMensaje tipo, int idNodoOrigen, int idNodoDestino, String contenido, Instant marcaTiempo) {
        this.tipo = tipo;
        this.idNodoOrigen = idNodoOrigen;
        this.idNodoDestino = idNodoDestino;
        this.contenido = contenido;
        this.marcaTiempo = marcaTiempo;
    }

    public TipoMensaje getTipo() {
        return tipo;
    }

    public void setTipo(TipoMensaje tipo) {
        this.tipo = tipo;
    }

    public int getIdNodoOrigen() {
        return idNodoOrigen;
    }

    public void setIdNodoOrigen(int idNodoOrigen) {
        this.idNodoOrigen = idNodoOrigen;
    }

    public int getIdNodoDestino() {
        return idNodoDestino;
    }

    public void setIdNodoDestino(int idNodoDestino) {
        this.idNodoDestino = idNodoDestino;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public Instant getMarcaTiempo() {
        return marcaTiempo;
    }

    public void setMarcaTiempo(Instant marcaTiempo) {
        this.marcaTiempo = marcaTiempo;
    }
}
