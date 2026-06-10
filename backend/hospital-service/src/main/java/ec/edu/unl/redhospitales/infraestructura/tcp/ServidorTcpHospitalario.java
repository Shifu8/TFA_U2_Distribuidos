/*
 * Abre el puerto TCP del nodo y entrega cada mensaje recibido al caso de uso distribuidor.
 */
package ec.edu.unl.redhospitales.infraestructura.tcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.redhospitales.aplicacion.servicio.ConfiguracionTcpLocal;
import ec.edu.unl.redhospitales.dominio.modelo.MensajeTcp;
import ec.edu.unl.redhospitales.dominio.puerto.PuertoManejadorMensajesTcp;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class ServidorTcpHospitalario {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServidorTcpHospitalario.class);

    private final ConfiguracionTcpLocal configuracionTcpLocal;
    private final ObjectMapper objectMapper;
    private final PuertoManejadorMensajesTcp manejadorMensajesTcp;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private volatile boolean ejecutando;
    private ServerSocket serverSocket;

    public ServidorTcpHospitalario(
            ConfiguracionTcpLocal configuracionTcpLocal,
            ObjectMapper objectMapper,
            PuertoManejadorMensajesTcp manejadorMensajesTcp
    ) {
        this.configuracionTcpLocal = configuracionTcpLocal;
        this.objectMapper = objectMapper;
        this.manejadorMensajesTcp = manejadorMensajesTcp;
    }

    @PostConstruct
    public void iniciar() {
        ejecutando = true;
        executor.submit(this::escuchar);
    }

    @PreDestroy
    public void detener() {
        ejecutando = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (Exception excepcion) {
            LOGGER.debug("Error cerrando servidor TCP: {}", excepcion.getMessage());
        }
        executor.shutdownNow();
    }

    private void escuchar() {
        try (ServerSocket socketServidor = new ServerSocket(configuracionTcpLocal.getPort())) {
            this.serverSocket = socketServidor;
            LOGGER.info("Servidor TCP hospitalario escuchando en puerto {}", configuracionTcpLocal.getPort());
            while (ejecutando) {
                Socket socket = socketServidor.accept();
                executor.submit(() -> atender(socket));
            }
        } catch (Exception excepcion) {
            if (ejecutando) {
                LOGGER.error("Servidor TCP detenido por error: {}", excepcion.getMessage(), excepcion);
            }
        }
    }

    private void atender(Socket socket) {
        try (socket;
             BufferedReader lector = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            String linea = lector.readLine();
            if (linea == null || linea.isBlank()) {
                return;
            }
            MensajeTcp mensaje = objectMapper.readValue(linea, MensajeTcp.class);
            manejadorMensajesTcp.manejar(mensaje);
        } catch (Exception excepcion) {
            LOGGER.warn("No se pudo procesar mensaje TCP: {}", excepcion.getMessage());
        }
    }
}
