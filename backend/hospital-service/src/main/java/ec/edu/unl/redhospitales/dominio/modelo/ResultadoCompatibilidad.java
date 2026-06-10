/*
 * Contiene el resultado de una evaluacion simulada de compatibilidad de donante.
 */
package ec.edu.unl.redhospitales.dominio.modelo;

public class ResultadoCompatibilidad {

    private boolean compatible;
    private String mensaje;
    private String coordinadorConsultado;
    private String urgencia;

    public ResultadoCompatibilidad() {
    }

    public ResultadoCompatibilidad(boolean compatible, String mensaje, String coordinadorConsultado, String urgencia) {
        this.compatible = compatible;
        this.mensaje = mensaje;
        this.coordinadorConsultado = coordinadorConsultado;
        this.urgencia = urgencia;
    }

    public boolean isCompatible() {
        return compatible;
    }

    public void setCompatible(boolean compatible) {
        this.compatible = compatible;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getCoordinadorConsultado() {
        return coordinadorConsultado;
    }

    public void setCoordinadorConsultado(String coordinadorConsultado) {
        this.coordinadorConsultado = coordinadorConsultado;
    }

    public String getUrgencia() {
        return urgencia;
    }

    public void setUrgencia(String urgencia) {
        this.urgencia = urgencia;
    }
}
