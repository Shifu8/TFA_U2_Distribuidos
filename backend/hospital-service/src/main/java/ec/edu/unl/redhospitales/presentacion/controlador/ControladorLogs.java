/*
 * Expone los eventos recientes del nodo consultado.
 */
package ec.edu.unl.redhospitales.presentacion.controlador;

import ec.edu.unl.redhospitales.aplicacion.servicio.ServicioRedHospitalaria;
import ec.edu.unl.redhospitales.presentacion.dto.EventoSistemaDto;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/api/logs")
public class ControladorLogs {

    private final ServicioRedHospitalaria servicioRedHospitalaria;

    public ControladorLogs(ServicioRedHospitalaria servicioRedHospitalaria) {
        this.servicioRedHospitalaria = servicioRedHospitalaria;
    }

    @GetMapping
    public List<EventoSistemaDto> listar() {
        return servicioRedHospitalaria.listarEventos().stream()
                .map(EventoSistemaDto::desde)
                .toList();
    }

    @GetMapping("/local")
    public List<EventoSistemaDto> listarLocales() {
        return servicioRedHospitalaria.listarEventosLocales().stream()
                .map(EventoSistemaDto::desde)
                .toList();
    }
}
