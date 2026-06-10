/*
 * Inicia el API Gateway que recibe solicitudes del frontend y las enruta al servicio hospitalario.
 */
package ec.edu.unl.redhospitales.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AplicacionApiGateway {

    public static void main(String[] args) {
        SpringApplication.run(AplicacionApiGateway.class, args);
    }
}
