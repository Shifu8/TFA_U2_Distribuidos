/*
 * Mantiene la identidad del nodo local recibida por argumentos o variables de entorno.
 */
package ec.edu.unl.redhospitales.aplicacion.servicio;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "node")
public class ConfiguracionNodoLocal {

    private static ConfiguracionNodoLocal instancia;

    private int id = 1;
    private String host = "localhost";

    @PostConstruct
    public void registrarInstancia() {
        instancia = this;
    }

    public static ConfiguracionNodoLocal obtenerInstancia() {
        if (instancia == null) {
            throw new IllegalStateException("ConfiguracionNodoLocal aun no fue inicializada");
        }
        return instancia;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }
}
