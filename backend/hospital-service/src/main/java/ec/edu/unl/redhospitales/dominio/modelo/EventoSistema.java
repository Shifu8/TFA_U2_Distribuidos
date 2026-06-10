/*
 * Guarda un evento legible para auditoria del comportamiento distribuido.
 */
package ec.edu.unl.redhospitales.dominio.modelo;

import java.time.Instant;

public class EventoSistema {

    private Instant fecha;
    private int nodoId;
    private String categoria;
    private String descripcion;

    public EventoSistema() {
    }

    public EventoSistema(Instant fecha, int nodoId, String categoria, String descripcion) {
        this.fecha = fecha;
        this.nodoId = nodoId;
        this.categoria = categoria;
        this.descripcion = descripcion;
    }

    public Instant getFecha() {
        return fecha;
    }

    public void setFecha(Instant fecha) {
        this.fecha = fecha;
    }

    public int getNodoId() {
        return nodoId;
    }

    public void setNodoId(int nodoId) {
        this.nodoId = nodoId;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
