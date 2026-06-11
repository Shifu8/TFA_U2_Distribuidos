/*
 * Representa un hospital participante de la red distribuida.
 */
package ec.edu.unl.redhospitales.dominio.modelo;

import ec.edu.unl.redhospitales.dominio.enumeracion.EstadoNodo;
import ec.edu.unl.redhospitales.dominio.enumeracion.RolNodo;

import java.time.Instant;
import java.util.Objects;

public class NodoHospitalario {

    private int id;
    private String nombreHospital;
    private String host;
    private int tcpPort;
    private int httpPort;
    private EstadoNodo estado;
    private RolNodo rol;
    private Instant ultimaSenal;

    public NodoHospitalario() {
        this.estado = EstadoNodo.ACTIVO;
        this.rol = RolNodo.SEGUIDOR;
        this.ultimaSenal = Instant.now();
    }

    public NodoHospitalario(int id, String nombreHospital, String host, int tcpPort, int httpPort) {
        this.id = id;
        this.nombreHospital = nombreHospital;
        this.host = host;
        this.tcpPort = tcpPort;
        this.httpPort = httpPort;
        this.estado = EstadoNodo.ACTIVO;
        this.rol = RolNodo.SEGUIDOR;
        this.ultimaSenal = Instant.now();
    }

    public NodoHospitalario copiar() {
        NodoHospitalario copia = new NodoHospitalario(id, nombreHospital, host, tcpPort, httpPort);
        copia.setEstado(estado);
        copia.setRol(rol);
        copia.setUltimaSenal(ultimaSenal);
        return copia;
    }

    public boolean esActivo() {
        return estado == EstadoNodo.ACTIVO;
    }

    public boolean esCoordinador() {
        return rol == RolNodo.COORDINADOR;
    }

    public void marcarActivo() {
        this.estado = EstadoNodo.ACTIVO;
        this.ultimaSenal = Instant.now();
    }

    public void marcarInactivo() {
        this.estado = EstadoNodo.INACTIVO;
        this.rol = RolNodo.SEGUIDOR;
    }

    public void actualizarSenal() {
        this.ultimaSenal = Instant.now();
        this.estado = EstadoNodo.ACTIVO;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombreHospital() {
        return nombreHospital;
    }

    public void setNombreHospital(String nombreHospital) {
        this.nombreHospital = nombreHospital;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getTcpPort() {
        return tcpPort;
    }

    public void setTcpPort(int tcpPort) {
        this.tcpPort = tcpPort;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    public EstadoNodo getEstado() {
        return estado;
    }

    public void setEstado(EstadoNodo estado) {
        this.estado = estado;
    }

    public RolNodo getRol() {
        return rol;
    }

    public void setRol(RolNodo rol) {
        this.rol = rol;
    }

    public Instant getUltimaSenal() {
        return ultimaSenal;
    }

    public void setUltimaSenal(Instant ultimaSenal) {
        this.ultimaSenal = ultimaSenal;
    }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) {
            return true;
        }
        if (!(objeto instanceof NodoHospitalario otro)) {
            return false;
        }
        return id == otro.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
