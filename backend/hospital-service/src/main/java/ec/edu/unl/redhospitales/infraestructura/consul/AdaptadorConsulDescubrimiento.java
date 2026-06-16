/*
 * Consulta instancias de hospital-service registradas en Consul mediante DiscoveryClient.
 */
package ec.edu.unl.redhospitales.infraestructura.consul;

import ec.edu.unl.redhospitales.dominio.puerto.PuertoDescubrimientoServicios;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    @Override
    public Set<Integer> obtenerNodosActivosEnConsul() {
        try {
            Set<Integer> ids = new HashSet<>();
            for (ServiceInstance instancia : discoveryClient.getInstances("hospital-service")) {
                String instanceId = instancia.getInstanceId();
                if (instanceId != null) {
                    try {
                        String[] partes = instanceId.split("-");
                        if (partes.length > 0) {
                            int id = Integer.parseInt(partes[partes.length - 1]);
                            ids.add(id);
                        }
                    } catch (NumberFormatException e) {
                        // Ignorar formato incorrecto
                    }
                }
            }
            return ids;
        } catch (RuntimeException excepcion) {
            return Collections.emptySet();
        }
    }

    private String describir(ServiceInstance instancia) {
        return instancia.getServiceId() + "@" + instancia.getHost() + ":" + instancia.getPort();
    }
}
