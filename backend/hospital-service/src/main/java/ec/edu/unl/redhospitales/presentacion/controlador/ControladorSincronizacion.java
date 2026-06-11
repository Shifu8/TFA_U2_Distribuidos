/*
 * Expone operaciones del algoritmo de Cristian.
 */
package ec.edu.unl.redhospitales.presentacion.controlador;

import ec.edu.unl.redhospitales.aplicacion.servicio.ServicioRedHospitalaria;
import ec.edu.unl.redhospitales.dominio.modelo.EstadoCristian;
import ec.edu.unl.redhospitales.presentacion.dto.RespuestaOperacionDto;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@RequestMapping({"/api/synchronization", "/api/sincronizacion", "/sincronizacion"})
public class ControladorSincronizacion {

    private final ServicioRedHospitalaria servicioRedHospitalaria;

    public ControladorSincronizacion(ServicioRedHospitalaria servicioRedHospitalaria) {
        this.servicioRedHospitalaria = servicioRedHospitalaria;
    }

    @PostMapping("/cristian")
    public RespuestaOperacionDto sincronizarCristian() {
        return new RespuestaOperacionDto(true, servicioRedHospitalaria.sincronizarCristianManual());
    }

    @PostMapping("/cristian/{nodeId}")
    public RespuestaOperacionDto sincronizarCristianEnNodo(@PathVariable int nodeId) {
        return new RespuestaOperacionDto(true, servicioRedHospitalaria.sincronizarCristianManual(nodeId));
    }

    @GetMapping("/cristian")
    public EstadoCristian estadoCristian() {
        return servicioRedHospitalaria.obtenerEstadoCristian();
    }
}
