/*
 * Consulta instancias de hospital-service registradas en Consul mediante DiscoveryClient.
 */
package ec.edu.unl.redhospitales.infraestructura.consul;

import ec.edu.unl.redhospitales.dominio.puerto.PuertoDescubrimientoServicios;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AdaptadorConsulDescubrimiento implements PuertoDescubrimientoServicios {

    private final DiscoveryClient discoveryClient;

    public AdaptadorConsulDescubrimiento(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    @Override
    public List<String> listarInstanciasHospitalService() {
        try {
            return discoveryClient.getInstances("hospital-service").stream()
                    .map(this::describir)
                    .toList();
        } catch (RuntimeException excepcion) {
            return List.of("Consul no disponible: " + excepcion.getMessage());
        }
    }

    private String describir(ServiceInstance instancia) {
        return instancia.getServiceId() + "@" + instancia.getHost() + ":" + instancia.getPort();
    }
}
