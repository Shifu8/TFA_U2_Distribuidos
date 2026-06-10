/*
 * Mantiene los parametros TCP del nodo local.
 */
package ec.edu.unl.redhospitales.aplicacion.servicio;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tcp")
public class ConfiguracionTcpLocal {

    private int port = 9001;
    private int connectTimeoutMs = 800;
    private int readTimeoutMs = 1200;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }
}
