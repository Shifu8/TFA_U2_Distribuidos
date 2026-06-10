/*
 * Carga los nodos hospitalarios desde nodes.json y aplica los parametros del nodo local.
 */
package ec.edu.unl.redhospitales.infraestructura.configuracion;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.redhospitales.aplicacion.servicio.ConfiguracionNodoLocal;
import ec.edu.unl.redhospitales.aplicacion.servicio.ConfiguracionTcpLocal;
import ec.edu.unl.redhospitales.dominio.modelo.NodoHospitalario;
import ec.edu.unl.redhospitales.dominio.puerto.PuertoConfiguracionNodos;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
public class AdaptadorConfiguracionNodosJson implements PuertoConfiguracionNodos {

    private final ObjectMapper objectMapper;
    private final Environment environment;
    private final ConfiguracionNodoLocal configuracionNodoLocal;
    private final ConfiguracionTcpLocal configuracionTcpLocal;

    public AdaptadorConfiguracionNodosJson(
            ObjectMapper objectMapper,
            Environment environment,
            ConfiguracionNodoLocal configuracionNodoLocal,
            ConfiguracionTcpLocal configuracionTcpLocal
    ) {
        this.objectMapper = objectMapper;
        this.environment = environment;
        this.configuracionNodoLocal = configuracionNodoLocal;
        this.configuracionTcpLocal = configuracionTcpLocal;
    }

    @Override
    public List<NodoHospitalario> cargarNodos() {
        String archivo = environment.getProperty("nodes.config-file", "nodes.json");
        try (InputStream entrada = abrirArchivo(archivo)) {
            ArchivoNodos archivoNodos = objectMapper.readValue(entrada, ArchivoNodos.class);
            List<NodoHospitalario> nodos = archivoNodos.nodos().stream()
                    .map(dto -> new NodoHospitalario(dto.id(), dto.nombreHospital(), dto.host(), dto.tcpPort(), dto.httpPort()))
                    .toList();
            aplicarSobrescrituraLocal(nodos);
            return nodos;
        } catch (IOException excepcion) {
            throw new IllegalStateException("No se pudo cargar nodes.json", excepcion);
        }
    }

    private InputStream abrirArchivo(String archivo) throws IOException {
        Path ruta = Path.of(archivo);
        if (Files.exists(ruta)) {
            return Files.newInputStream(ruta);
        }
        return new ClassPathResource("nodes.json").getInputStream();
    }

    private void aplicarSobrescrituraLocal(List<NodoHospitalario> nodos) {
        int puertoHttp = environment.getProperty("server.port", Integer.class, 8081);
        for (NodoHospitalario nodo : nodos) {
            if (nodo.getId() == configuracionNodoLocal.getId()) {
                nodo.setHost(configuracionNodoLocal.getHost());
                nodo.setTcpPort(configuracionTcpLocal.getPort());
                nodo.setHttpPort(puertoHttp);
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ArchivoNodos(List<NodoJson> nodos) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NodoJson(int id, String nombreHospital, String host, int tcpPort, int httpPort) {
    }
}
