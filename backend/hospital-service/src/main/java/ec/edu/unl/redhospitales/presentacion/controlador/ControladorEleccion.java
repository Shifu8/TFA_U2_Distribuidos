/*
 * Permite iniciar una eleccion Bully de forma manual desde la API.
 */
package ec.edu.unl.redhospitales.presentacion.controlador;

import ec.edu.unl.redhospitales.aplicacion.servicio.ServicioRedHospitalaria;
import ec.edu.unl.redhospitales.presentacion.dto.RespuestaOperacionDto;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@RequestMapping({"/api/election", "/eleccion"})
public class ControladorEleccion {

    private final ServicioRedHospitalaria servicioRedHospitalaria;

    public ControladorEleccion(ServicioRedHospitalaria servicioRedHospitalaria) {
        this.servicioRedHospitalaria = servicioRedHospitalaria;
    }

    @PostMapping({"/start", "/iniciar"})
    public RespuestaOperacionDto iniciar() {
        return new RespuestaOperacionDto(true, servicioRedHospitalaria.iniciarEleccionManual());
    }
}
