/*
 * Describe una consulta simulada de compatibilidad entre donante y receptor.
 */
package ec.edu.unl.redhospitales.dominio.modelo;

public class SolicitudDonante {

    private String nombreDonante;
    private String tipoSangreDonante;
    private String nombreReceptor;
    private String tipoSangreReceptor;
    private String urgencia;

    public SolicitudDonante() {
    }

    public SolicitudDonante(String nombreDonante, String tipoSangreDonante, String nombreReceptor, String tipoSangreReceptor, String urgencia) {
        this.nombreDonante = nombreDonante;
        this.tipoSangreDonante = tipoSangreDonante;
        this.nombreReceptor = nombreReceptor;
        this.tipoSangreReceptor = tipoSangreReceptor;
        this.urgencia = urgencia;
    }

    public String getNombreDonante() {
        return nombreDonante;
    }

    public void setNombreDonante(String nombreDonante) {
        this.nombreDonante = nombreDonante;
    }

    public String getTipoSangreDonante() {
        return tipoSangreDonante;
    }

    public void setTipoSangreDonante(String tipoSangreDonante) {
        this.tipoSangreDonante = tipoSangreDonante;
    }

    public String getNombreReceptor() {
        return nombreReceptor;
    }

    public void setNombreReceptor(String nombreReceptor) {
        this.nombreReceptor = nombreReceptor;
    }

    public String getTipoSangreReceptor() {
        return tipoSangreReceptor;
    }

    public void setTipoSangreReceptor(String tipoSangreReceptor) {
        this.tipoSangreReceptor = tipoSangreReceptor;
    }

    public String getUrgencia() {
        return urgencia;
    }

    public void setUrgencia(String urgencia) {
        this.urgencia = urgencia;
    }
}
