/*
 * Expone una respuesta sencilla para verificar el API Gateway desde el navegador.
 */
package ec.edu.unl.redhospitales.gateway;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ControladorEstadoGateway {

    @GetMapping("/")
    public Map<String, Object> inicio() {
        return Map.of(
                "servicio", "API Gateway - Red de Hospitales",
                "estado", "ACTIVO",
                "dashboard", "Abrir el frontend en el puerto 5173 de la PC principal",
                "rutas", Map.of(
                        "nodos", "/api/nodes",
                        "coordinador", "/api/nodes/coordinator",
                        "consul", "/api/nodes/consul",
                        "salud", "/actuator/health"
                )
        );
    }
}
