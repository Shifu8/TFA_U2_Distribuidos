/*
 * Expone la exclusion mutua por servidor central.
 */
package ec.edu.unl.redhospitales.presentacion.controlador;

import ec.edu.unl.redhospitales.aplicacion.servicio.ServicioRedHospitalaria;
import ec.edu.unl.redhospitales.dominio.modelo.EstadoExclusionMutua;
import ec.edu.unl.redhospitales.presentacion.dto.RespuestaOperacionDto;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@RequestMapping({"/api/exclusion", "/exclusion"})
public class ControladorExclusion {

    private final ServicioRedHospitalaria servicioRedHospitalaria;

    public ControladorExclusion(ServicioRedHospitalaria servicioRedHospitalaria) {
        this.servicioRedHospitalaria = servicioRedHospitalaria;
    }

    @PostMapping({"/request", "/solicitar"})
    public RespuestaOperacionDto solicitar() {
        return new RespuestaOperacionDto(true, servicioRedHospitalaria.solicitarExclusionMutua());
    }

    @PostMapping({"/request/{nodeId}", "/solicitar/{nodeId}"})
    public RespuestaOperacionDto solicitarEnNodo(@PathVariable int nodeId) {
        return new RespuestaOperacionDto(true, servicioRedHospitalaria.solicitarExclusionMutua(nodeId));
    }

    @PostMapping({"/release", "/liberar"})
    public RespuestaOperacionDto liberar() {
        return new RespuestaOperacionDto(true, servicioRedHospitalaria.liberarExclusionMutua());
    }

    @PostMapping({"/release/{nodeId}", "/liberar/{nodeId}"})
    public RespuestaOperacionDto liberarEnNodo(@PathVariable int nodeId) {
        return new RespuestaOperacionDto(true, servicioRedHospitalaria.liberarExclusionMutua(nodeId));
    }

    @GetMapping({"/status", "/estado"})
    public EstadoExclusionMutua estado() {
        return servicioRedHospitalaria.obtenerEstadoExclusionMutua();
    }

    @GetMapping("/local")
    public EstadoExclusionMutua estadoLocal() {
        return servicioRedHospitalaria.obtenerEstadoExclusionMutuaLocal();
    }
}
