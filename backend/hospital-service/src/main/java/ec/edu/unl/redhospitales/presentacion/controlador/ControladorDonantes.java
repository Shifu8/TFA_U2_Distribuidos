/*
 * Recibe consultas simuladas de compatibilidad de donantes desde el frontend.
 */
package ec.edu.unl.redhospitales.presentacion.controlador;

import ec.edu.unl.redhospitales.aplicacion.servicio.ServicioRedHospitalaria;
import ec.edu.unl.redhospitales.presentacion.dto.ResultadoCompatibilidadDto;
import ec.edu.unl.redhospitales.presentacion.dto.SolicitudDonanteDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@RequestMapping("/api/donors")
public class ControladorDonantes {

    private final ServicioRedHospitalaria servicioRedHospitalaria;

    public ControladorDonantes(ServicioRedHospitalaria servicioRedHospitalaria) {
        this.servicioRedHospitalaria = servicioRedHospitalaria;
    }

    @PostMapping("/compatibility")
    public ResultadoCompatibilidadDto compatibilidad(@Valid @RequestBody SolicitudDonanteDto solicitud) {
        return ResultadoCompatibilidadDto.desde(servicioRedHospitalaria.consultarCompatibilidad(solicitud.aModelo()));
    }
}
