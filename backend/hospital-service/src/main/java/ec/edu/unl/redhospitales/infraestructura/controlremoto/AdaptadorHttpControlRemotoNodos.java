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
    private final ec.edu.unl.redhospitales.infraestructura.soporte.GestorCortocircuitos gestorCortocircuitos;

    public AdaptadorHttpControlRemotoNodos(RestTemplate restTemplate, ec.edu.unl.redhospitales.infraestructura.soporte.GestorCortocircuitos gestorCortocircuitos) {
        this.restTemplate = restTemplate;
        this.gestorCortocircuitos = gestorCortocircuitos;
    }

    @Override
    public boolean solicitarCaida(NodoHospitalario nodo) {
        return ejecutarPost(nodo, "/api/simulation/local/fail");
    }

    @Override
    public boolean solicitarRecuperacion(NodoHospitalario nodo) {
        return ejecutarPost(nodo, "/api/simulation/local/recover");
    }

    @Override
    public boolean solicitarSincronizacionCristian(NodoHospitalario nodo) {
        return ejecutarPost(nodo, "/api/synchronization/cristian");
    }

    @Override
    public boolean solicitarExclusion(NodoHospitalario nodo) {
        return ejecutarPost(nodo, "/api/exclusion/request");
    }

    @Override
    public boolean liberarExclusion(NodoHospitalario nodo) {
        return ejecutarPost(nodo, "/api/exclusion/release");
    }

    private boolean ejecutarPost(NodoHospitalario nodo, String ruta) {
        if (!gestorCortocircuitos.permitirLlamada(nodo.getId())) {
            LOGGER.debug("[CIRCUIT BREAKER] Solicitud HTTP cancelada al nodo {} ({}) - Circuito ABIERTO", nodo.getId(), ruta);
            return false;
        }
        String url = "http://" + nodo.getHost() + ":" + nodo.getHttpPort() + ruta;
        try {
            ResponseEntity<String> respuesta = restTemplate.postForEntity(url, null, String.class);
            boolean exito = respuesta.getStatusCode().is2xxSuccessful();
            if (exito) {
                gestorCortocircuitos.registrarExito(nodo.getId());
            } else {
                gestorCortocircuitos.registrarFallo(nodo.getId());
            }
            return exito;
        } catch (RuntimeException excepcion) {
            LOGGER.warn("No se pudo llamar a {}: {}", url, excepcion.getMessage());
            gestorCortocircuitos.registrarFallo(nodo.getId());
            return false;
        }
    }
}
