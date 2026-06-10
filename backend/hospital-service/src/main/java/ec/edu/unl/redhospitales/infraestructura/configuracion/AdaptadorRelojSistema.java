/*
 * Provee la hora actual del sistema operativo.
 */
package ec.edu.unl.redhospitales.infraestructura.configuracion;

import ec.edu.unl.redhospitales.dominio.puerto.PuertoReloj;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class AdaptadorRelojSistema implements PuertoReloj {

    @Override
    public Instant ahora() {
        return Instant.now();
    }
}
