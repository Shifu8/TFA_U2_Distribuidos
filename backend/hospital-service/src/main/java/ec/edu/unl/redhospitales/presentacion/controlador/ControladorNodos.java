/*
 * Expone endpoints para consultar nodos, coordinador e instancias descubiertas por Consul.
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
@RequestMapping("/api/nodes")
public class ControladorNodos {

    private final ServicioRedHospitalaria servicioRedHospitalaria;

    public ControladorNodos(ServicioRedHospitalaria servicioRedHospitalaria) {
        this.servicioRedHospitalaria = servicioRedHospitalaria;
    }

    @GetMapping
    public List<NodoHospitalarioDto> listar() {
        return servicioRedHospitalaria.listarNodos().stream()
                .map(NodoHospitalarioDto::desde)
                .toList();
    }

    @GetMapping("/coordinator")
    public NodoHospitalarioDto coordinador() {
        return servicioRedHospitalaria.obtenerCoordinador()
                .map(NodoHospitalarioDto::desde)
                .orElseThrow(() -> new IllegalStateException("No existe coordinador actual"));
    }

    @GetMapping("/local")
    public NodoHospitalarioDto local() {
        return NodoHospitalarioDto.desde(servicioRedHospitalaria.obtenerNodoLocal());
    }

    @GetMapping("/consul")
    public List<String> consul() {
        return servicioRedHospitalaria.listarInstanciasConsul();
    }
}
