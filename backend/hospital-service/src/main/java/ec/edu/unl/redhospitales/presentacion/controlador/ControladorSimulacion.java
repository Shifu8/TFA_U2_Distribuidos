/*
 * Expone acciones para simular caida y recuperacion de nodos hospitalarios.
 */
package ec.edu.unl.redhospitales.presentacion.controlador;

import ec.edu.unl.redhospitales.aplicacion.servicio.ServicioRedHospitalaria;
import ec.edu.unl.redhospitales.presentacion.dto.RespuestaOperacionDto;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@RequestMapping("/api/simulation")
public class ControladorSimulacion {

    private final ServicioRedHospitalaria servicioRedHospitalaria;

    public ControladorSimulacion(ServicioRedHospitalaria servicioRedHospitalaria) {
        this.servicioRedHospitalaria = servicioRedHospitalaria;
    }

    @PostMapping("/fail/{nodeId}")
    public RespuestaOperacionDto fallar(@PathVariable int nodeId) {
        return new RespuestaOperacionDto(true, servicioRedHospitalaria.simularCaida(nodeId));
    }

    @PostMapping("/recover/{nodeId}")
    public RespuestaOperacionDto recuperar(@PathVariable int nodeId) {
        return new RespuestaOperacionDto(true, servicioRedHospitalaria.simularRecuperacion(nodeId));
    }

    @PostMapping("/local/fail")
    public RespuestaOperacionDto fallarLocal() {
        servicioRedHospitalaria.aplicarCaidaLocal();
        return new RespuestaOperacionDto(true, "Nodo local marcado como INACTIVO");
    }

    @PostMapping("/local/recover")
    public RespuestaOperacionDto recuperarLocal() {
        servicioRedHospitalaria.aplicarRecuperacionLocal();
        return new RespuestaOperacionDto(true, "Nodo local recuperado");
    }
}
