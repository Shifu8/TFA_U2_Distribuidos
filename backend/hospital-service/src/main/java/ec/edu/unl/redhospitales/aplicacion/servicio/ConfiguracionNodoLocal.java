/*
 * Mantiene la identidad del nodo local recibida por argumentos o variables de entorno.
 */
package ec.edu.unl.redhospitales.aplicacion.servicio;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "node")
public class ConfiguracionNodoLocal {

    private int id = 1;
    private String host = "localhost";

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
