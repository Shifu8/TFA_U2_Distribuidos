/*
 * Expone la lista de hospitales configurados para el dashboard.
 */
package ec.edu.unl.redhospitales.presentacion.controlador;

import ec.edu.unl.redhospitales.aplicacion.servicio.ServicioRedHospitalaria;
import ec.edu.unl.redhospitales.presentacion.dto.NodoHospitalarioDto;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/api/hospitals")
public class ControladorHospitales {

    private final ServicioRedHospitalaria servicioRedHospitalaria;

    public ControladorHospitales(ServicioRedHospitalaria servicioRedHospitalaria) {
        this.servicioRedHospitalaria = servicioRedHospitalaria;
    }

    @GetMapping
    public List<NodoHospitalarioDto> listarHospitales() {
        return servicioRedHospitalaria.listarNodos().stream()
                .map(NodoHospitalarioDto::desde)
                .toList();
    }
}
