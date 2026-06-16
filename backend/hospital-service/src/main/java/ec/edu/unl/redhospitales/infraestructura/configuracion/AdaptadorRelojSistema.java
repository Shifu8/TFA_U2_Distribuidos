/*
 * Provee la hora actual del sistema operativo.
 */
package ec.edu.unl.redhospitales.infraestructura.configuracion;

import ec.edu.unl.redhospitales.dominio.puerto.PuertoReloj;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class AdaptadorRelojSistema implements PuertoReloj {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdaptadorRelojSistema.class);

    @Override
    public Instant ahora() {
        return Instant.now();
    }

    @Override
    public boolean ajustarSistema(Instant nuevaHora) {
        if (esLinux()) {
            return ajustarLinux(nuevaHora);
        }
        if (esWindows()) {
            return ajustarWindows(nuevaHora);
        }

        LOGGER.warn("Ajuste de reloj del sistema omitido: sistema operativo no soportado para ajuste automatico");
        return false;
    }

    private boolean ajustarLinux(Instant nuevaHora) {
        ejecutarIgnorandoError(comandoPrivilegiado("timedatectl", "set-ntp", "false"));
        boolean ajustado = ejecutar(comandoPrivilegiado("date", "-u", "-s", "@" + nuevaHora.getEpochSecond()));
        if (!ajustado) {
            LOGGER.warn("No se pudo ajustar el reloj del sistema. Ejecuta el nodo con sudo o configura sudo sin password para date/timedatectl.");
        }
        return ajustado;
    }

    private boolean ajustarWindows(Instant nuevaHora) {
        String script = "$fecha=[DateTime]'1970-01-01T00:00:00Z';"
                + "Set-Date -Date $fecha.AddMilliseconds(" + nuevaHora.toEpochMilli() + ").ToLocalTime()";
        boolean ajustado = ejecutar(List.of(
                "powershell.exe",
                "-NoProfile",
                "-ExecutionPolicy",
                "Bypass",
                "-Command",
                script
        ));
        if (!ajustado) {
            LOGGER.warn("No se pudo ajustar el reloj del sistema en Windows. Ejecuta PowerShell como Administrador.");
        }
        return ajustado;
    }

    private boolean esLinux() {
        return System.getProperty("os.name", "").toLowerCase().contains("linux");
    }

    private boolean esWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("windows");
    }

    private List<String> comandoPrivilegiado(String... comando) {
        if (esRoot()) {
            return List.of(comando);
        }
        List<String> resultado = new java.util.ArrayList<>();
        resultado.add("sudo");
        resultado.add("-n");
        resultado.addAll(List.of(comando));
        return resultado;
    }

    private boolean esRoot() {
        try {
            Process proceso = new ProcessBuilder("id", "-u").start();
            String salida = new String(proceso.getInputStream().readAllBytes()).trim();
            return proceso.waitFor() == 0 && salida.equals("0");
        } catch (Exception excepcion) {
            return false;
        }
    }

    private void ejecutarIgnorandoError(List<String> comando) {
        ejecutar(comando);
    }

    private boolean ejecutar(List<String> comando) {
        try {
            Process proceso = new ProcessBuilder(comando).redirectErrorStream(true).start();
            boolean terminado = proceso.waitFor(4, java.util.concurrent.TimeUnit.SECONDS);
            return terminado && proceso.exitValue() == 0;
        } catch (Exception excepcion) {
            LOGGER.debug("Error ejecutando comando de reloj {}: {}", comando, excepcion.getMessage());
            return false;
        }
    }
}
