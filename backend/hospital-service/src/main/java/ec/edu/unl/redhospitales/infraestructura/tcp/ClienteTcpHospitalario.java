/*
 * Envia mensajes JSON de una linea a otros nodos mediante sockets TCP.
 */
package ec.edu.unl.redhospitales.infraestructura.tcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.redhospitales.aplicacion.servicio.ConfiguracionTcpLocal;
import ec.edu.unl.redhospitales.dominio.modelo.MensajeTcp;
import ec.edu.unl.redhospitales.dominio.modelo.NodoHospitalario;
import ec.edu.unl.redhospitales.dominio.puerto.PuertoComunicacionTcp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

@Component
public class ClienteTcpHospitalario implements PuertoComunicacionTcp {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClienteTcpHospitalario.class);

    private final ObjectMapper objectMapper;
    private final ConfiguracionTcpLocal configuracionTcpLocal;

    public ClienteTcpHospitalario(ObjectMapper objectMapper, ConfiguracionTcpLocal configuracionTcpLocal) {
        this.objectMapper = objectMapper;
        this.configuracionTcpLocal = configuracionTcpLocal;
    }

    @Override
    public boolean enviar(MensajeTcp mensaje, NodoHospitalario destino) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(destino.getHost(), destino.getTcpPort()), configuracionTcpLocal.getConnectTimeoutMs());
            socket.setSoTimeout(configuracionTcpLocal.getReadTimeoutMs());
            try (BufferedWriter escritor = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
                escritor.write(objectMapper.writeValueAsString(mensaje));
                escritor.newLine();
                escritor.flush();
            }
            return true;
        } catch (Exception excepcion) {
            LOGGER.debug("Fallo al enviar {} al nodo {}:{} - {}", mensaje.getTipo(), destino.getHost(), destino.getTcpPort(), excepcion.getMessage());
            return false;
        }
    }
}
