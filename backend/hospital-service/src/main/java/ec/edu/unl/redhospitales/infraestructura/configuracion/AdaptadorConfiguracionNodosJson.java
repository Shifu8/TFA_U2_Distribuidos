/*
 * Carga los nodos hospitalarios desde nodes.json y aplica los parametros del nodo local.
 */
package ec.edu.unl.redhospitales.infraestructura.configuracion;

import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.ArrayList;
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
            JsonNode raiz = objectMapper.readTree(entrada);
            JsonNode nodosJson = raiz.isArray() ? raiz : raiz.path("nodos");
            if (!nodosJson.isArray()) {
                throw new IllegalStateException("nodes.json debe contener un arreglo o una propiedad 'nodos'");
            }

            List<NodoHospitalario> nodos = new ArrayList<>();
            for (JsonNode nodoJson : nodosJson) {
                int id = nodoJson.path("id").asInt();
                String nombreHospital = texto(nodoJson, "nombreHospital", "Hospital Nodo " + id);
                String host = texto(nodoJson, "host", "localhost");
                int tcpPort = entero(nodoJson, "tcpPort", "port", 9000 + id);
                int httpPort = entero(nodoJson, "httpPort", "apiPort", 8080 + id);
                nodos.add(new NodoHospitalario(id, nombreHospital, host, tcpPort, httpPort));
            }
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

    private String texto(JsonNode nodo, String campo, String defecto) {
        JsonNode valor = nodo.get(campo);
        return valor == null || valor.asText().isBlank() ? defecto : valor.asText();
    }

    private int entero(JsonNode nodo, String campoPrincipal, String campoAlterno, int defecto) {
        if (nodo.has(campoPrincipal)) {
            return nodo.path(campoPrincipal).asInt(defecto);
        }
        return nodo.path(campoAlterno).asInt(defecto);
    }
}
