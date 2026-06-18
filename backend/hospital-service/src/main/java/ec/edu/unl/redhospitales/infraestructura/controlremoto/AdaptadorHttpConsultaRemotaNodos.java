/*
 * Consulta endpoints locales de cada nodo para consolidar estado y logs del cluster.
 */
package ec.edu.unl.redhospitales.infraestructura.controlremoto;

import ec.edu.unl.redhospitales.dominio.enumeracion.EstadoNodo;
import ec.edu.unl.redhospitales.dominio.enumeracion.RolNodo;
import ec.edu.unl.redhospitales.dominio.modelo.EventoSistema;
import ec.edu.unl.redhospitales.dominio.modelo.EstadoExclusionMutua;
import ec.edu.unl.redhospitales.dominio.modelo.NodoHospitalario;
import ec.edu.unl.redhospitales.dominio.puerto.PuertoConsultaRemotaNodos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class AdaptadorHttpConsultaRemotaNodos implements PuertoConsultaRemotaNodos {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdaptadorHttpConsultaRemotaNodos.class);

    private final RestTemplate restTemplate;
    private final ec.edu.unl.redhospitales.infraestructura.soporte.GestorCortocircuitos gestorCortocircuitos;

    public AdaptadorHttpConsultaRemotaNodos(RestTemplate restTemplate, ec.edu.unl.redhospitales.infraestructura.soporte.GestorCortocircuitos gestorCortocircuitos) {
        this.restTemplate = restTemplate;
        this.gestorCortocircuitos = gestorCortocircuitos;
    }

    @Override
    public Optional<NodoHospitalario> consultarEstadoLocal(NodoHospitalario nodo) {
        if (!gestorCortocircuitos.permitirLlamada(nodo.getId())) {
            LOGGER.debug("[CIRCUIT BREAKER] Consulta HTTP cancelada al nodo {} (EstadoLocal) - Circuito ABIERTO", nodo.getId());
            return Optional.empty();
        }
        String url = url(nodo, "/api/nodes/local");
        try {
            NodoRemotoDto dto = restTemplate.getForObject(url, NodoRemotoDto.class);
            gestorCortocircuitos.registrarExito(nodo.getId());
            return dto == null ? Optional.empty() : Optional.of(dto.aModelo());
        } catch (RuntimeException excepcion) {
            LOGGER.debug("No se pudo consultar estado local en {}: {}", url, excepcion.getMessage());
            gestorCortocircuitos.registrarFallo(nodo.getId());
            return Optional.empty();
        }
    }

    @Override
    public List<EventoSistema> consultarEventosLocales(NodoHospitalario nodo) {
        if (!gestorCortocircuitos.permitirLlamada(nodo.getId())) {
            LOGGER.debug("[CIRCUIT BREAKER] Consulta HTTP cancelada al nodo {} (EventosLocales) - Circuito ABIERTO", nodo.getId());
            return List.of();
        }
        String url = url(nodo, "/api/logs/local");
        try {
            EventoRemotoDto[] respuesta = restTemplate.getForObject(url, EventoRemotoDto[].class);
            gestorCortocircuitos.registrarExito(nodo.getId());
            if (respuesta == null) {
                return List.of();
            }
            return Arrays.stream(respuesta)
                    .map(EventoRemotoDto::aModelo)
                    .toList();
        } catch (RuntimeException excepcion) {
            LOGGER.debug("No se pudo consultar logs locales en {}: {}", url, excepcion.getMessage());
            gestorCortocircuitos.registrarFallo(nodo.getId());
            return List.of();
        }
    }

    @Override
    public Optional<EstadoExclusionMutua> consultarEstadoExclusionLocal(NodoHospitalario nodo) {
        if (!gestorCortocircuitos.permitirLlamada(nodo.getId())) {
            LOGGER.debug("[CIRCUIT BREAKER] Consulta HTTP cancelada al nodo {} (ExclusionLocal) - Circuito ABIERTO", nodo.getId());
            return Optional.empty();
        }
        String url = url(nodo, "/api/exclusion/local");
        try {
            EstadoExclusionMutua estado = restTemplate.getForObject(url, EstadoExclusionMutua.class);
            gestorCortocircuitos.registrarExito(nodo.getId());
            return Optional.ofNullable(estado);
        } catch (RuntimeException excepcion) {
            LOGGER.debug("No se pudo consultar exclusion mutua local en {}: {}", url, excepcion.getMessage());
            gestorCortocircuitos.registrarFallo(nodo.getId());
            return Optional.empty();
        }
    }

    private String url(NodoHospitalario nodo, String ruta) {
        return "http://" + nodo.getHost() + ":" + nodo.getHttpPort() + ruta;
    }

    private record NodoRemotoDto(
            int id,
            String nombreHospital,
            String host,
            int tcpPort,
            int httpPort,
            String estado,
            String rol,
            Instant ultimaSenal
    ) {
        NodoHospitalario aModelo() {
            NodoHospitalario nodo = new NodoHospitalario(id, nombreHospital, host, tcpPort, httpPort);
            nodo.setEstado(EstadoNodo.valueOf(estado));
            nodo.setRol(RolNodo.valueOf(rol));
            nodo.setUltimaSenal(ultimaSenal);
            return nodo;
        }
    }

    private record EventoRemotoDto(
            Instant fecha,
            int nodoId,
            String categoria,
            String descripcion
    ) {
        EventoSistema aModelo() {
            return new EventoSistema(fecha, nodoId, categoria, descripcion);
        }
    }
}
