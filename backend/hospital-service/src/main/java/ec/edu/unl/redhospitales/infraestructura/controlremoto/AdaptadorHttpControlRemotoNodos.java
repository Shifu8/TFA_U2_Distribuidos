/*
 * Invoca endpoints REST locales de otros nodos para simular caida o recuperacion.
 */
package ec.edu.unl.redhospitales.infraestructura.controlremoto;

import ec.edu.unl.redhospitales.dominio.modelo.NodoHospitalario;
import ec.edu.unl.redhospitales.dominio.puerto.PuertoControlRemotoNodos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class AdaptadorHttpControlRemotoNodos implements PuertoControlRemotoNodos {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdaptadorHttpControlRemotoNodos.class);

    private final RestTemplate restTemplate;

    public AdaptadorHttpControlRemotoNodos(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public boolean solicitarCaida(NodoHospitalario nodo) {
        return ejecutarPost(nodo, "/api/simulation/local/fail");
    }

    @Override
    public boolean solicitarRecuperacion(NodoHospitalario nodo) {
        return ejecutarPost(nodo, "/api/simulation/local/recover");
    }

    private boolean ejecutarPost(NodoHospitalario nodo, String ruta) {
        String url = "http://" + nodo.getHost() + ":" + nodo.getHttpPort() + ruta;
        try {
            ResponseEntity<String> respuesta = restTemplate.postForEntity(url, null, String.class);
            return respuesta.getStatusCode().is2xxSuccessful();
        } catch (RuntimeException excepcion) {
            LOGGER.warn("No se pudo llamar a {}: {}", url, excepcion.getMessage());
            return false;
        }
    }
}
