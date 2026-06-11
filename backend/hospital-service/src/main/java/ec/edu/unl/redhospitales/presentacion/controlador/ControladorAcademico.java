/*
 * Alias REST en espanol para los endpoints solicitados en la defensa.
 */
package ec.edu.unl.redhospitales.presentacion.controlador;

import ec.edu.unl.redhospitales.aplicacion.servicio.ServicioRedHospitalaria;
import ec.edu.unl.redhospitales.presentacion.dto.EstadoSistemaDto;
import ec.edu.unl.redhospitales.presentacion.dto.NodoHospitalarioDto;
import ec.edu.unl.redhospitales.presentacion.dto.ResultadoCompatibilidadDto;
import ec.edu.unl.redhospitales.presentacion.dto.SolicitudDonanteDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin
@RestController
public class ControladorAcademico {

    private final ServicioRedHospitalaria servicioRedHospitalaria;

    public ControladorAcademico(ServicioRedHospitalaria servicioRedHospitalaria) {
        this.servicioRedHospitalaria = servicioRedHospitalaria;
    }

    @GetMapping({"/estado", "/api/estado", "/api/status"})
    public EstadoSistemaDto estado() {
        List<NodoHospitalarioDto> nodos = servicioRedHospitalaria.listarNodos().stream()
                .map(NodoHospitalarioDto::desde)
                .toList();
        NodoHospitalarioDto coordinador = servicioRedHospitalaria.obtenerCoordinador()
                .map(NodoHospitalarioDto::desde)
                .orElse(null);
        long activos = nodos.stream()
                .filter(nodo -> nodo.estado().equals("ACTIVO"))
                .count();
        return new EstadoSistemaDto(
                NodoHospitalarioDto.desde(servicioRedHospitalaria.obtenerNodoLocal()),
                coordinador,
                activos,
                nodos,
                servicioRedHospitalaria.obtenerEstadoCristian(),
                servicioRedHospitalaria.obtenerEstadoExclusionMutua()
        );
    }

    @GetMapping("/nodos")
    public List<NodoHospitalarioDto> nodos() {
        return servicioRedHospitalaria.listarNodos().stream()
                .map(NodoHospitalarioDto::desde)
                .toList();
    }

    @GetMapping("/coordinador")
    public NodoHospitalarioDto coordinador() {
        return servicioRedHospitalaria.obtenerCoordinador()
                .map(NodoHospitalarioDto::desde)
                .orElseThrow(() -> new IllegalStateException("No existe coordinador actual"));
    }

    @PostMapping({"/solicitar_organo", "/api/solicitar_organo"})
    public ResultadoCompatibilidadDto solicitarOrgano(@Valid @RequestBody SolicitudDonanteDto solicitud) {
        return ResultadoCompatibilidadDto.desde(servicioRedHospitalaria.consultarCompatibilidad(solicitud.aModelo()));
    }
}
