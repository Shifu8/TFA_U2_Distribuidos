/*
 * Define una fuente de tiempo para facilitar la sincronizacion y pruebas.
 */
package ec.edu.unl.redhospitales.dominio.puerto;

import java.time.Instant;

public interface PuertoReloj {

    Instant ahora();
}
