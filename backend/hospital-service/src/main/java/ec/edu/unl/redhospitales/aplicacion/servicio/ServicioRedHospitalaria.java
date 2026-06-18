/*
 * Coordina nodos, Bully, heartbeats, Cristian, simulacion de fallos y consultas de donantes.
 */
package ec.edu.unl.redhospitales.aplicacion.servicio;

import ec.edu.unl.redhospitales.dominio.enumeracion.EstadoNodo;
import ec.edu.unl.redhospitales.dominio.enumeracion.RolNodo;
import ec.edu.unl.redhospitales.dominio.enumeracion.TipoMensaje;
import ec.edu.unl.redhospitales.dominio.modelo.EventoSistema;
import ec.edu.unl.redhospitales.dominio.modelo.EstadoCristian;
import ec.edu.unl.redhospitales.dominio.modelo.EstadoExclusionMutua;
import ec.edu.unl.redhospitales.dominio.modelo.MensajeTcp;
import ec.edu.unl.redhospitales.dominio.modelo.NodoHospitalario;
import ec.edu.unl.redhospitales.dominio.modelo.ResultadoCompatibilidad;
import ec.edu.unl.redhospitales.dominio.modelo.SolicitudDonante;
import ec.edu.unl.redhospitales.dominio.puerto.PuertoComunicacionTcp;
import ec.edu.unl.redhospitales.dominio.puerto.PuertoConfiguracionNodos;
import ec.edu.unl.redhospitales.dominio.puerto.PuertoConsultaRemotaNodos;
import ec.edu.unl.redhospitales.dominio.puerto.PuertoControlRemotoNodos;
import ec.edu.unl.redhospitales.dominio.puerto.PuertoDescubrimientoServicios;
import ec.edu.unl.redhospitales.dominio.puerto.PuertoManejadorMensajesTcp;
import ec.edu.unl.redhospitales.dominio.puerto.PuertoRegistroEventos;
import ec.edu.unl.redhospitales.dominio.puerto.PuertoReloj;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ServicioRedHospitalaria implements PuertoManejadorMensajesTcp {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServicioRedHospitalaria.class);

    private final PuertoConfiguracionNodos configuracionNodos;
    private final PuertoComunicacionTcp comunicacionTcp;
    private final PuertoRegistroEventos registroEventos;
    private final PuertoDescubrimientoServicios descubrimientoServicios;
    private final PuertoControlRemotoNodos controlRemotoNodos;
    private final PuertoConsultaRemotaNodos consultaRemotaNodos;
    private final PuertoReloj reloj;
    private final ConfiguracionNodoLocal configuracionNodoLocal;
    private final ConfiguracionCoordinacion configuracionCoordinacion;
    private final Map<Integer, NodoHospitalario> nodos = new ConcurrentHashMap<>();
    private final Set<Integer> respuestasEleccion = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> solicitudesCristian = new ConcurrentHashMap<>();
    private final AtomicBoolean eleccionEnCurso = new AtomicBoolean(false);
    private final GestorConcurrencia gestorConcurrencia = GestorConcurrencia.obtenerInstancia();

    private volatile boolean nodoLocalOperativo = true;
    private volatile int idCoordinadorActual = -1;
    private volatile EstadoCristian ultimoEstadoCristian = new EstadoCristian();
    private volatile boolean solicitudExclusionPendiente = false;
    private volatile boolean accesoExclusionConcedido = false;

    private static final String RECURSO_CRITICO = "directorio-donantes";

    public ServicioRedHospitalaria(
            PuertoConfiguracionNodos configuracionNodos,
            PuertoComunicacionTcp comunicacionTcp,
            PuertoRegistroEventos registroEventos,
            PuertoDescubrimientoServicios descubrimientoServicios,
            PuertoControlRemotoNodos controlRemotoNodos,
            PuertoConsultaRemotaNodos consultaRemotaNodos,
            PuertoReloj reloj,
            ConfiguracionNodoLocal configuracionNodoLocal,
            ConfiguracionCoordinacion configuracionCoordinacion
    ) {
        this.configuracionNodos = configuracionNodos;
        this.comunicacionTcp = comunicacionTcp;
        this.registroEventos = registroEventos;
        this.descubrimientoServicios = descubrimientoServicios;
        this.controlRemotoNodos = controlRemotoNodos;
        this.consultaRemotaNodos = consultaRemotaNodos;
        this.reloj = reloj;
        this.configuracionNodoLocal = configuracionNodoLocal;
        this.configuracionCoordinacion = configuracionCoordinacion;
    }

    @PostConstruct
    public void inicializar() {
        List<NodoHospitalario> configurados = configuracionNodos.cargarNodos();
        if (configurados.isEmpty()) {
            throw new IllegalStateException("No existen nodos configurados en nodes.json");
        }

        configurados.forEach(nodo -> nodos.put(nodo.getId(), nodo));
        if (!nodos.containsKey(configuracionNodoLocal.getId())) {
            throw new IllegalStateException("El node.id=" + configuracionNodoLocal.getId() + " no existe en nodes.json");
        }

        idCoordinadorActual = nodos.keySet().stream().max(Integer::compareTo).orElse(configuracionNodoLocal.getId());
        aplicarRolCoordinador(idCoordinadorActual);
        if (esCoordinadorLocal()) {
            gestorConcurrencia.reiniciar();
        }
        registrar("INICIO", "Nodo " + configuracionNodoLocal.getId() + " iniciado. Coordinador inicial: nodo " + idCoordinadorActual);

        // Sincronizar Cristian una sola vez al iniciar (despues de 5 segundos para que la red este lista)
        CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS).execute(() -> {
            try {
                solicitarSincronizacionCristian("sincronizacion inicial al arrancar el nodo");
            } catch (Exception e) {
                LOGGER.error("Error al realizar la sincronizacion Cristian inicial", e);
            }
        });
    }

    public List<NodoHospitalario> listarNodos() {
        return listarNodosConsolidados();
    }

    public Optional<NodoHospitalario> obtenerCoordinador() {
        List<NodoHospitalario> consolidados = listarNodosConsolidados();
        Optional<NodoHospitalario> coordinadorDeclarado = consolidados.stream()
                .filter(NodoHospitalario::esActivo)
                .filter(NodoHospitalario::esCoordinador)
                .max(Comparator.comparingInt(NodoHospitalario::getId));
        if (coordinadorDeclarado.isPresent()) {
            return coordinadorDeclarado;
        }
        return consolidados.stream()
                .filter(NodoHospitalario::esActivo)
                .max(Comparator.comparingInt(NodoHospitalario::getId));
    }

    public List<EventoSistema> listarEventos() {
        Map<String, EventoSistema> eventos = new LinkedHashMap<>();
        agregarEventos(eventos, registroEventos.listar());
        nodos.values().stream()
                .filter(nodo -> nodo.getId() != configuracionNodoLocal.getId())
                .filter(nodo -> nodo.getEstado() != EstadoNodo.INACTIVO)
                .forEach(nodo -> agregarEventos(eventos, consultaRemotaNodos.consultarEventosLocales(nodo)));

        return eventos.values().stream()
                .sorted(Comparator.comparing(EventoSistema::getFecha).reversed())
                .limit(configuracionCoordinacion.getMaximoLogs())
                .toList();
    }

    public List<EventoSistema> listarEventosLocales() {
        return registroEventos.listar();
    }

    public List<EventoSistema> listarEventos(String algoritmo) {
        List<EventoSistema> eventos = listarEventos();
        if (algoritmo == null || algoritmo.isBlank() || algoritmo.equalsIgnoreCase("todos")) {
            return eventos;
        }

        String categoria = algoritmo.trim().toUpperCase(Locale.ROOT);
        return eventos.stream()
                .filter(evento -> coincideCategoria(evento.getCategoria(), categoria))
                .toList();
    }

    public NodoHospitalario obtenerNodoLocal() {
        return nodoLocal().copiar();
    }

    public List<String> listarInstanciasConsul() {
        return descubrimientoServicios.listarInstanciasHospitalService();
    }

    public String iniciarEleccionManual() {
        iniciarEleccion("eleccion manual solicitada por API");
        return "Eleccion Bully solicitada desde nodo " + configuracionNodoLocal.getId();
    }

    public String sincronizarCristianManual() {
        if (solicitarSincronizacionCristian("sincronizacion manual solicitada por API")) {
            return "Sincronizacion Cristian solicitada desde nodo " + configuracionNodoLocal.getId();
        }
        return "No se pudo solicitar sincronizacion Cristian desde nodo " + configuracionNodoLocal.getId();
    }

    public String sincronizarCristianManual(int nodeId) {
        if (nodeId == configuracionNodoLocal.getId()) {
            return sincronizarCristianManual();
        }

        NodoHospitalario nodo = obtenerNodoObligatorio(nodeId);
        if (nodo.getEstado() == EstadoNodo.INACTIVO) {
            return "No se puede solicitar sincronizacion Cristian en nodo " + nodeId + " porque está INACTIVO";
        }
        boolean enviada = controlRemotoNodos.solicitarSincronizacionCristian(nodo);
        registrar("CRISTIAN", "Solicitud remota de sincronizacion enviada al nodo " + nodeId + ". Resultado: " + enviada);
        return enviada
                ? "Sincronizacion Cristian solicitada en nodo " + nodeId
                : "No se pudo solicitar sincronizacion Cristian en nodo " + nodeId;
    }

    public EstadoCristian obtenerEstadoCristian() {
        EstadoCristian estado = ultimoEstadoCristian;
        return new EstadoCristian(
                reloj.ahora(),
                estado.getHoraCoordinador(),
                estado.getHoraAjustada(),
                estado.getRttMs(),
                estado.getLatenciaMs(),
                configuracionCoordinacion.isAjustarRelojSistema(),
                estado.isRelojSistemaAjustado()
        );
    }

    public String solicitarExclusionMutua() {
        if (!estaOperativo()) {
            return "Nodo local inactivo: no puede solicitar seccion critica";
        }

        if (esCoordinadorLocal()) {
            registrar("EXCLUSION", "Nodo coordinador solicita acceso local a " + RECURSO_CRITICO);
            procesarSolicitudMutexLocal(configuracionNodoLocal.getId());
            return "Solicitud de exclusion mutua procesada por el coordinador local";
        }

        Optional<NodoHospitalario> coordinador = obtenerCoordinador();
        if (coordinador.isEmpty()) {
            return "No existe coordinador disponible para solicitar exclusion mutua";
        }

        solicitudExclusionPendiente = true;
        boolean enviada = enviarMensaje(coordinador.get(), TipoMensaje.MUTEX_REQUEST, RECURSO_CRITICO + "|" + UUID.randomUUID());
        if (!enviada) {
            solicitudExclusionPendiente = false;
            return "No se pudo enviar MUTEX_REQUEST al coordinador";
        }

        registrar("EXCLUSION", "MUTEX_REQUEST enviado al coordinador nodo " + coordinador.get().getId());
        return "Solicitud de seccion critica enviada al coordinador nodo " + coordinador.get().getId();
    }

    public String solicitarExclusionMutua(int nodeId) {
        if (nodeId == configuracionNodoLocal.getId()) {
            return solicitarExclusionMutua();
        }

        NodoHospitalario nodo = obtenerNodoObligatorio(nodeId);
        if (nodo.getEstado() == EstadoNodo.INACTIVO) {
            return "No se puede solicitar seccion critica en nodo " + nodeId + " porque está INACTIVO";
        }
        boolean enviada = controlRemotoNodos.solicitarExclusion(nodo);
        registrar("EXCLUSION", "Solicitud remota de exclusion enviada al nodo " + nodeId + ". Resultado: " + enviada);
        return enviada
                ? "Solicitud de seccion critica enviada al nodo " + nodeId
                : "No se pudo solicitar seccion critica en nodo " + nodeId;
    }

    public String liberarExclusionMutua() {
        if (!estaOperativo()) {
            return "Nodo local inactivo: no puede liberar seccion critica";
        }

        if (esCoordinadorLocal()) {
            liberarMutexDesdeNodo(configuracionNodoLocal.getId());
            return "Seccion critica liberada desde el coordinador local";
        }

        Optional<NodoHospitalario> coordinador = obtenerCoordinador();
        if (coordinador.isEmpty()) {
            return "No existe coordinador disponible para liberar exclusion mutua";
        }

        accesoExclusionConcedido = false;
        solicitudExclusionPendiente = false;
        boolean enviada = enviarMensaje(coordinador.get(), TipoMensaje.MUTEX_RELEASE, RECURSO_CRITICO);
        if (!enviada) {
            return "No se pudo enviar MUTEX_RELEASE al coordinador";
        }

        registrar("EXCLUSION", "MUTEX_RELEASE enviado al coordinador nodo " + coordinador.get().getId());
        return "Liberacion de seccion critica enviada al coordinador nodo " + coordinador.get().getId();
    }

    public String liberarExclusionMutua(int nodeId) {
        if (nodeId == configuracionNodoLocal.getId()) {
            return liberarExclusionMutua();
        }

        NodoHospitalario nodo = obtenerNodoObligatorio(nodeId);
        if (nodo.getEstado() == EstadoNodo.INACTIVO) {
            return "No se puede liberar seccion critica en nodo " + nodeId + " porque está INACTIVO";
        }
        boolean enviada = controlRemotoNodos.liberarExclusion(nodo);
        registrar("EXCLUSION", "Solicitud remota de liberacion enviada al nodo " + nodeId + ". Resultado: " + enviada);
        return enviada
                ? "Liberacion de seccion critica enviada al nodo " + nodeId
                : "No se pudo liberar seccion critica en nodo " + nodeId;
    }

    public EstadoExclusionMutua obtenerEstadoExclusionMutua() {
        if (esCoordinadorLocal()) {
            return obtenerEstadoExclusionMutuaLocal();
        }

        return obtenerCoordinador()
                .filter(nodo -> nodo.getId() != configuracionNodoLocal.getId())
                .filter(nodo -> nodo.getEstado() != EstadoNodo.INACTIVO)
                .flatMap(consultaRemotaNodos::consultarEstadoExclusionLocal)
                .orElseGet(this::obtenerEstadoExclusionMutuaLocal);
    }

    public EstadoExclusionMutua obtenerEstadoExclusionMutuaLocal() {
        Integer nodoEnSeccion = esCoordinadorLocal() ? gestorConcurrencia.getNodoEnSeccionCritica() : null;
        List<Integer> cola = esCoordinadorLocal() ? gestorConcurrencia.snapshotCola() : List.of();
        boolean ocupado = nodoEnSeccion != null;
        String estadoLocal;
        if (accesoExclusionConcedido) {
            estadoLocal = "EN_SECCION_CRITICA";
        } else if (solicitudExclusionPendiente) {
            estadoLocal = "ESPERANDO";
        } else {
            estadoLocal = "LIBRE";
        }

        return new EstadoExclusionMutua(
                configuracionNodoLocal.getId(),
                idCoordinadorActual,
                esCoordinadorLocal(),
                ocupado,
                nodoEnSeccion,
                cola,
                solicitudExclusionPendiente,
                accesoExclusionConcedido,
                RECURSO_CRITICO,
                estadoLocal
        );
    }

    public String simularCaida(int nodeId) {
        NodoHospitalario nodo = obtenerNodoObligatorio(nodeId);
        if (nodeId == configuracionNodoLocal.getId()) {
            aplicarCaidaLocal();
            return "Nodo local " + nodeId + " marcado como INACTIVO";
        }

        boolean enviada = controlRemotoNodos.solicitarCaida(nodo);
        nodo.marcarInactivo();
        registrar("SIMULACION", "Solicitud de caida enviada al nodo " + nodeId + ". Resultado remoto: " + enviada);

        if (nodeId == idCoordinadorActual && estaOperativo()) {
            iniciarEleccion("coordinador remoto marcado como inactivo");
        }

        return enviada
                ? "Nodo " + nodeId + " marcado como INACTIVO"
                : "Nodo " + nodeId + " marcado localmente; no se pudo confirmar la llamada remota";
    }

    public String simularRecuperacion(int nodeId) {
        NodoHospitalario nodo = obtenerNodoObligatorio(nodeId);
        if (nodeId == configuracionNodoLocal.getId()) {
            aplicarRecuperacionLocal();
            return "Nodo local " + nodeId + " recuperado";
        }

        boolean enviada = controlRemotoNodos.solicitarRecuperacion(nodo);
        nodo.marcarActivo();
        registrar("SIMULACION", "Solicitud de recuperacion enviada al nodo " + nodeId + ". Resultado remoto: " + enviada);
        return enviada
                ? "Nodo " + nodeId + " recuperado"
                : "Nodo " + nodeId + " marcado localmente; no se pudo confirmar la llamada remota";
    }

    public void aplicarCaidaLocal() {
        NodoHospitalario local = nodoLocal();
        nodoLocalOperativo = false;
        local.marcarInactivo();
        registrar("SIMULACION", "Nodo local " + local.getId() + " simula caida: no envia heartbeats ni responde TCP distribuido");
    }

    public void aplicarRecuperacionLocal() {
        NodoHospitalario local = nodoLocal();
        nodoLocalOperativo = true;
        local.marcarActivo();
        registrar("SIMULACION", "Nodo local " + local.getId() + " recuperado y listo para participar");
        iniciarEleccion("nodo recuperado se reincorpora a la red");
    }

    public ResultadoCompatibilidad consultarCompatibilidad(SolicitudDonante solicitud) {
        boolean compatible = esCompatible(solicitud.getTipoSangreDonante(), solicitud.getTipoSangreReceptor());
        String mensaje = compatible
                ? "Donante compatible para la consulta simulada"
                : "Donante no compatible para la consulta simulada";
        String coordinador = obtenerCoordinador()
                .map(NodoHospitalario::getNombreHospital)
                .orElse("Sin coordinador");

        ResultadoCompatibilidad resultado = new ResultadoCompatibilidad(compatible, mensaje, coordinador, solicitud.getUrgencia());
        registrar("DONANTES", "Consulta de donante " + normalizar(solicitud.getTipoSangreDonante())
                + " -> receptor " + normalizar(solicitud.getTipoSangreReceptor()) + ": " + mensaje);

        if (estaOperativo() && !nodoLocal().esCoordinador()) {
            obtenerCoordinador().ifPresent(nodo -> {
                String contenido = String.join("|",
                        valorSeguro(solicitud.getNombreDonante()),
                        valorSeguro(solicitud.getTipoSangreDonante()),
                        valorSeguro(solicitud.getNombreReceptor()),
                        valorSeguro(solicitud.getTipoSangreReceptor()),
                        valorSeguro(solicitud.getUrgencia()));
                enviarMensaje(nodo, TipoMensaje.DONOR_REQUEST, contenido);
            });
        }

        return resultado;
    }

    @Override
    public void manejar(MensajeTcp mensaje) {
        if (!estaOperativo()) {
            return;
        }
        if (mensaje.getIdNodoDestino() != configuracionNodoLocal.getId()) {
            return;
        }

        NodoHospitalario origen = nodos.get(mensaje.getIdNodoOrigen());
        if (origen != null) {
            origen.actualizarSenal();
        }

        switch (mensaje.getTipo()) {
            case HEARTBEAT -> procesarHeartbeat(mensaje);
            case ELECTION -> procesarElection(mensaje);
            case ANSWER -> procesarAnswer(mensaje);
            case COORDINATOR -> procesarCoordinator(mensaje);
            case SYNC_TIME_REQUEST -> procesarSyncTimeRequest(mensaje);
            case SYNC_TIME_RESPONSE -> procesarSyncTimeResponse(mensaje);
            case MUTEX_REQUEST -> procesarMutexRequest(mensaje);
            case MUTEX_GRANT -> procesarMutexGrant(mensaje);
            case MUTEX_RELEASE -> procesarMutexRelease(mensaje);
            case DONOR_REQUEST -> procesarDonorRequest(mensaje);
            case DONOR_RESPONSE -> registrar("DONANTES", "Respuesta de donante recibida desde nodo " + mensaje.getIdNodoOrigen()
                    + ": " + mensaje.getContenido());
        }
    }

    @Scheduled(fixedDelayString = "${coordinacion.heartbeat-ms:2000}", initialDelay = 1500)
    public void enviarHeartbeatsProgramados() {
        if (!estaOperativo() || !esCoordinadorLocal()) {
            return;
        }

        nodos.values().stream()
                .filter(nodo -> nodo.getId() != configuracionNodoLocal.getId())
                .forEach(nodo -> enviarMensaje(nodo, TipoMensaje.HEARTBEAT, "heartbeat desde coordinador " + configuracionNodoLocal.getId()));
    }

    @Scheduled(fixedDelay = 1000, initialDelay = 2500)
    public void verificarCoordinadorProgramado() {
        if (!estaOperativo() || esCoordinadorLocal()) {
            return;
        }

        NodoHospitalario coordinador = nodos.get(idCoordinadorActual);
        if (coordinador == null) {
            iniciarEleccion("no existe coordinador conocido");
            return;
        }

        if (coordinador.getId() == configuracionNodoLocal.getId()) {
            aplicarRolCoordinador(configuracionNodoLocal.getId());
            return;
        }

        long silencioMs = Duration.between(coordinador.getUltimaSenal(), reloj.ahora()).toMillis();
        if (silencioMs > configuracionCoordinacion.getTimeoutHeartbeatMs()) {
            coordinador.marcarSospechoso();
            registrar("HEARTBEAT", "No se recibio heartbeat del coordinador " + coordinador.getId()
                    + " durante " + silencioMs + " ms. Se marca como SOSPECHOSO, no como INACTIVO.");
            iniciarEleccion("timeout de heartbeat del coordinador");
        }
    }

    @Scheduled(fixedDelay = 2000, initialDelay = 3000)
    public void sincronizarEstadoNodosConConsul() {
        if (!estaOperativo()) {
            return;
        }

        try {
            Set<Integer> nodosActivosConsul = descubrimientoServicios.obtenerNodosActivosEnConsul();
            if (nodosActivosConsul.isEmpty()) {
                return;
            }

            boolean coordinadorSeCayo = false;

            for (NodoHospitalario nodo : nodos.values()) {
                if (nodo.getId() == configuracionNodoLocal.getId()) {
                    continue;
                }

                boolean activoEnConsul = nodosActivosConsul.contains(nodo.getId());
                if (activoEnConsul) {
                    if (nodo.getEstado() == EstadoNodo.INACTIVO) {
                        nodo.marcarActivo();
                        registrar("CONSUL", "Nodo " + nodo.getId() + " detectado como ACTIVO a través de Consul.");
                    }
                } else {
                    if (nodo.getEstado() != EstadoNodo.INACTIVO) {
                        nodo.marcarInactivo();
                        registrar("CONSUL", "Nodo " + nodo.getId() + " detectado como INACTIVO a través de Consul.");
                        if (nodo.getId() == idCoordinadorActual) {
                            coordinadorSeCayo = true;
                        }
                    }
                }
            }

            if (coordinadorSeCayo) {
                registrar("BULLY", "El coordinador (nodo " + idCoordinadorActual + ") se ha caído según Consul. Iniciando elección...");
                iniciarEleccion("coordinador caido detectado por Consul");
            }
        } catch (Exception e) {
            LOGGER.error("Error al sincronizar estado de nodos con Consul", e);
        }
    }

    private void iniciarEleccion(String motivo) {
        if (!estaOperativo()) {
            registrar("BULLY", "No se inicia eleccion porque el nodo local esta inactivo");
            return;
        }
        if (!eleccionEnCurso.compareAndSet(false, true)) {
            registrar("BULLY", "Eleccion ya en curso. Motivo ignorado: " + motivo);
            return;
        }

        respuestasEleccion.clear();
        nodoLocal().setRol(RolNodo.SEGUIDOR);
        registrar("BULLY", "Nodo " + configuracionNodoLocal.getId() + " inicia eleccion. Motivo: " + motivo);

        List<NodoHospitalario> superiores = nodos.values().stream()
            .filter(nodo -> nodo.getId() > configuracionNodoLocal.getId())
            .filter(NodoHospitalario::esActivo)
            .sorted(Comparator.comparingInt(NodoHospitalario::getId))
            .toList();

        if (superiores.isEmpty()) {
            registrar("BULLY", "No hay nodos superiores activos. Nodo " + configuracionNodoLocal.getId() + " se convierte inmediatamente en coordinador.");
            eleccionEnCurso.set(false);
            convertirseEnCoordinador();
            return;
        }

        superiores.forEach(nodo -> enviarMensaje(nodo, TipoMensaje.ELECTION, "eleccion iniciada por nodo " + configuracionNodoLocal.getId()));

        CompletableFuture.delayedExecutor(
                configuracionCoordinacion.getEsperaRespuestaEleccionMs(),
                TimeUnit.MILLISECONDS
        ).execute(this::cerrarVentanaEleccion);
    }

    private void cerrarVentanaEleccion() {
        if (!estaOperativo()) {
            eleccionEnCurso.set(false);
            return;
        }

        if (respuestasEleccion.isEmpty()) {
            eleccionEnCurso.set(false);
            convertirseEnCoordinador();
            return;
        }

        registrar("BULLY", "Nodo " + configuracionNodoLocal.getId()
                + " recibio ANSWER de " + new ArrayList<>(respuestasEleccion)
                + " y espera anuncio COORDINATOR");
        eleccionEnCurso.set(false);
    }

    private void convertirseEnCoordinador() {
        if (!estaOperativo()) {
            return;
        }

        idCoordinadorActual = configuracionNodoLocal.getId();
        gestorConcurrencia.reiniciar();
        solicitudExclusionPendiente = false;
        accesoExclusionConcedido = false;
        aplicarRolCoordinador(idCoordinadorActual);
        registrar("BULLY", "Nodo " + configuracionNodoLocal.getId() + " se declara COORDINADOR");

        nodos.values().stream()
                .filter(nodo -> nodo.getId() != configuracionNodoLocal.getId())
                .forEach(nodo -> enviarMensaje(nodo, TipoMensaje.COORDINATOR, "nuevo coordinador " + configuracionNodoLocal.getId()));
    }

    private void procesarHeartbeat(MensajeTcp mensaje) {
        NodoHospitalario origen = nodos.get(mensaje.getIdNodoOrigen());
        if (origen == null) {
            return;
        }

        if (origen.getEstado() == EstadoNodo.INACTIVO) {
            registrar("HEARTBEAT", "Ignorado heartbeat de nodo INACTIVO: " + mensaje.getIdNodoOrigen());
            return;
        }

        if (nodoLocal().esCoordinador() && mensaje.getIdNodoOrigen() < configuracionNodoLocal.getId()) {
            registrar("HEARTBEAT", "Heartbeat de coordinador menor recibido. Se conserva coordinador local");
            return;
        }

        boolean coordinadorCambiado = (idCoordinadorActual != mensaje.getIdNodoOrigen());
        idCoordinadorActual = mensaje.getIdNodoOrigen();
        aplicarRolCoordinador(idCoordinadorActual);
        origen.actualizarSenal();
        if (coordinadorCambiado) {
            registrar("HEARTBEAT", "Nuevo coordinador detectado via heartbeat: nodo " + mensaje.getIdNodoOrigen());
        }
    }

    private void procesarElection(MensajeTcp mensaje) {
        registrar("BULLY", "Mensaje ELECTION recibido desde nodo " + mensaje.getIdNodoOrigen());
        if (configuracionNodoLocal.getId() > mensaje.getIdNodoOrigen()) {
            NodoHospitalario destino = nodos.get(mensaje.getIdNodoOrigen());
            if (destino != null) {
                enviarMensaje(destino, TipoMensaje.ANSWER, "nodo " + configuracionNodoLocal.getId() + " responde ANSWER");
            }
            if (!esCoordinadorLocal()) {
                iniciarEleccion("solicitud recibida desde nodo con ID menor");
            }
        }
    }

    private void procesarAnswer(MensajeTcp mensaje) {
        respuestasEleccion.add(mensaje.getIdNodoOrigen());
        registrar("BULLY", "ANSWER recibido desde nodo " + mensaje.getIdNodoOrigen());
    }

    private void procesarCoordinator(MensajeTcp mensaje) {
        if (mensaje.getIdNodoOrigen() < configuracionNodoLocal.getId()) {
            registrar("BULLY", "Anuncio COORDINATOR de nodo menor " + mensaje.getIdNodoOrigen()
                    + " rechazado por nodo " + configuracionNodoLocal.getId());
            iniciarEleccion("coordinador menor anunciado");
            return;
        }

        idCoordinadorActual = mensaje.getIdNodoOrigen();
        aplicarRolCoordinador(idCoordinadorActual);
        eleccionEnCurso.set(false);
        solicitudExclusionPendiente = false;
        accesoExclusionConcedido = false;
        NodoHospitalario coordinador = nodos.get(idCoordinadorActual);
        if (coordinador != null) {
            coordinador.actualizarSenal();
        }
        registrar("BULLY", "Nodo " + idCoordinadorActual + " anunciado como nuevo COORDINADOR");
    }

    private void procesarSyncTimeRequest(MensajeTcp mensaje) {
        if (!nodoLocal().esCoordinador()) {
            return;
        }
        NodoHospitalario destino = nodos.get(mensaje.getIdNodoOrigen());
        if (destino == null) {
            return;
        }
        long horaCoordinador = reloj.ahora().toEpochMilli();
        enviarMensaje(destino, TipoMensaje.SYNC_TIME_RESPONSE, mensaje.getContenido() + "|" + horaCoordinador);
        registrar("CRISTIAN", "Solicitud de tiempo atendida para nodo " + mensaje.getIdNodoOrigen());
    }

    private void procesarSyncTimeResponse(MensajeTcp mensaje) {
        String[] partes = mensaje.getContenido().split("\\|");
        if (partes.length != 2) {
            return;
        }

        Long envioMs = solicitudesCristian.remove(partes[0]);
        if (envioMs == null) {
            return;
        }

        long recepcionMs = System.currentTimeMillis();
        long rtt = Math.max(0, recepcionMs - envioMs);
        long latencia = rtt / 2;
        Instant horaLocal = Instant.ofEpochMilli(recepcionMs);
        Instant horaCoordinador = Instant.ofEpochMilli(Long.parseLong(partes[1]));
        Instant horaAjustada = horaCoordinador.plusMillis(latencia);
        boolean relojSistemaAjustado = false;
        if (configuracionCoordinacion.isAjustarRelojSistema()) {
            relojSistemaAjustado = reloj.ajustarSistema(horaAjustada);
        }
        ultimoEstadoCristian = new EstadoCristian(
                horaLocal,
                horaCoordinador,
                horaAjustada,
                rtt,
                latencia,
                configuracionCoordinacion.isAjustarRelojSistema(),
                relojSistemaAjustado
        );

        registrar("CRISTIAN", "Hora local=" + horaLocal
                + ", hora coordinador=" + horaCoordinador
                + ", latencia estimada=" + latencia + " ms"
                + ", hora ajustada=" + horaAjustada
                + ", ajuste sistema=" + relojSistemaAjustado);
    }

    private void procesarMutexRequest(MensajeTcp mensaje) {
        if (!esCoordinadorLocal()) {
            registrar("EXCLUSION", "MUTEX_REQUEST recibido en nodo no coordinador. Origen: nodo " + mensaje.getIdNodoOrigen());
            return;
        }

        registrar("EXCLUSION", "MUTEX_REQUEST recibido desde nodo " + mensaje.getIdNodoOrigen());
        procesarSolicitudMutexLocal(mensaje.getIdNodoOrigen());
    }

    private void procesarMutexGrant(MensajeTcp mensaje) {
        solicitudExclusionPendiente = false;
        accesoExclusionConcedido = true;
        registrar("EXCLUSION", "MUTEX_GRANT recibido desde coordinador nodo " + mensaje.getIdNodoOrigen()
                + ". Nodo entra a seccion critica: " + RECURSO_CRITICO);
    }

    private void procesarMutexRelease(MensajeTcp mensaje) {
        if (!esCoordinadorLocal()) {
            registrar("EXCLUSION", "MUTEX_RELEASE recibido en nodo no coordinador. Origen: nodo " + mensaje.getIdNodoOrigen());
            return;
        }

        liberarMutexDesdeNodo(mensaje.getIdNodoOrigen());
    }

    private void procesarSolicitudMutexLocal(int nodoId) {
        GestorConcurrencia.ResultadoSolicitud resultado = gestorConcurrencia.solicitarAcceso(nodoId);
        if (resultado.concedido()) {
            otorgarAccesoMutex(nodoId);
            return;
        }

        registrar("EXCLUSION", "Nodo " + nodoId + " agregado a cola FIFO. Cola actual: " + resultado.colaEspera());
    }

    private void liberarMutexDesdeNodo(int nodoId) {
        GestorConcurrencia.ResultadoLiberacion resultado = gestorConcurrencia.liberarAcceso(nodoId);
        if (nodoId == configuracionNodoLocal.getId()) {
            accesoExclusionConcedido = false;
            solicitudExclusionPendiente = false;
        }

        if (resultado.liberado()) {
            registrar("EXCLUSION", "Nodo " + nodoId + " libera seccion critica. Cola FIFO: " + resultado.colaEspera());
        } else {
            registrar("EXCLUSION", "Nodo " + nodoId + " no tenia la seccion critica. Cola FIFO: " + resultado.colaEspera());
        }

        if (resultado.liberado() && resultado.siguienteNodo() != null) {
            otorgarAccesoMutex(resultado.siguienteNodo());
        }
    }

    private void otorgarAccesoMutex(int nodoId) {
        if (nodoId == configuracionNodoLocal.getId()) {
            solicitudExclusionPendiente = false;
            accesoExclusionConcedido = true;
            registrar("EXCLUSION", "Coordinador concede acceso local a seccion critica: " + RECURSO_CRITICO);
            return;
        }

        NodoHospitalario destino = nodos.get(nodoId);
        if (destino == null) {
            return;
        }

        boolean enviado = enviarMensaje(destino, TipoMensaje.MUTEX_GRANT, RECURSO_CRITICO);
        registrar("EXCLUSION", "Coordinador concede acceso a nodo " + nodoId + ". Enviado: " + enviado);
    }

    private void procesarDonorRequest(MensajeTcp mensaje) {
        if (!nodoLocal().esCoordinador()) {
            return;
        }

        String[] partes = mensaje.getContenido().split("\\|", -1);
        String tipoDonante = partes.length > 1 ? partes[1] : "";
        String tipoReceptor = partes.length > 3 ? partes[3] : "";
        boolean compatible = esCompatible(tipoDonante, tipoReceptor);
        String resultado = compatible ? "compatible" : "no compatible";
        registrar("DONANTES", "Coordinador procesa DONOR_REQUEST del nodo " + mensaje.getIdNodoOrigen()
                + ": " + normalizar(tipoDonante) + " -> " + normalizar(tipoReceptor) + " = " + resultado);

        NodoHospitalario destino = nodos.get(mensaje.getIdNodoOrigen());
        if (destino != null) {
            enviarMensaje(destino, TipoMensaje.DONOR_RESPONSE, resultado);
        }
    }

    private boolean enviarMensaje(NodoHospitalario destino, TipoMensaje tipo, String contenido) {
        if (destino == null || destino.getId() == configuracionNodoLocal.getId()) {
            return false;
        }
        if (destino.getEstado() == EstadoNodo.INACTIVO) {
            return false;
        }

        MensajeTcp mensaje = new MensajeTcp(
                tipo,
                configuracionNodoLocal.getId(),
                destino.getId(),
                contenido,
                reloj.ahora()
        );

        boolean enviado = comunicacionTcp.enviar(mensaje, destino);
        if (!enviado && tipo != TipoMensaje.HEARTBEAT) {
            registrar("TCP", "No se pudo enviar " + tipo + " al nodo " + destino.getId());
        }
        if (!enviado) {
            destino.marcarSospechoso();
        }
        return enviado;
    }

    private void aplicarRolCoordinador(int idCoordinador) {
        nodos.values().forEach(nodo -> nodo.setRol(nodo.getId() == idCoordinador ? RolNodo.COORDINADOR : RolNodo.SEGUIDOR));
        NodoHospitalario coordinador = nodos.get(idCoordinador);
        if (coordinador != null) {
            coordinador.setEstado(EstadoNodo.ACTIVO);
            coordinador.actualizarSenal();
        }
    }

    private List<NodoHospitalario> listarNodosConsolidados() {
        return nodos.values().stream()
                .sorted(Comparator.comparingInt(NodoHospitalario::getId))
                .map(this::consultarVistaActual)
                .toList();
    }

    private NodoHospitalario consultarVistaActual(NodoHospitalario nodo) {
        if (nodo.getId() == configuracionNodoLocal.getId()) {
            return nodo.copiar();
        }
        if (nodo.getEstado() == EstadoNodo.INACTIVO) {
            return nodo.copiar();
        }

        Optional<NodoHospitalario> remoto = consultaRemotaNodos.consultarEstadoLocal(nodo);
        if (remoto.isPresent()) {
            actualizarNodo(nodos.get(remoto.get().getId()), remoto.get());
            return remoto.get().copiar();
        }

        NodoHospitalario copia = nodo.copiar();
        copia.marcarSospechoso();
        return copia;
    }

    private void actualizarNodo(NodoHospitalario destino, NodoHospitalario fuente) {
        if (destino == null) {
            return;
        }
        destino.setNombreHospital(fuente.getNombreHospital());
        destino.setHost(fuente.getHost());
        destino.setTcpPort(fuente.getTcpPort());
        destino.setHttpPort(fuente.getHttpPort());
        destino.setEstado(fuente.getEstado());
        destino.setRol(fuente.getRol());
        destino.actualizarSenal();
    }

    private boolean solicitarSincronizacionCristian(String motivo) {
        if (!estaOperativo()) {
            return false;
        }

        if (esCoordinadorLocal()) {
            Instant ahora = reloj.ahora();
            ultimoEstadoCristian = new EstadoCristian(
                    ahora,
                    ahora,
                    ahora,
                    0,
                    0,
                    configuracionCoordinacion.isAjustarRelojSistema(),
                    false
            );
            registrar("CRISTIAN", "Nodo coordinador usa su propio reloj como referencia. Motivo: " + motivo);
            return true;
        }

        NodoHospitalario coordinador = nodos.get(idCoordinadorActual);
        if (coordinador == null || !coordinador.esActivo()) {
            return false;
        }

        String solicitudId = UUID.randomUUID().toString();
        solicitudesCristian.put(solicitudId, System.currentTimeMillis());
        boolean enviado = enviarMensaje(coordinador, TipoMensaje.SYNC_TIME_REQUEST, solicitudId);
        if (enviado) {
            registrar("CRISTIAN", "SYNC_TIME_REQUEST enviado al coordinador nodo " + coordinador.getId()
                    + ". Motivo: " + motivo);
        }
        return enviado;
    }

    private boolean coincideCategoria(String categoriaEvento, String categoriaSolicitada) {
        if (categoriaSolicitada.equals("RED")) {
            return categoriaEvento.equalsIgnoreCase("TCP") || categoriaEvento.equalsIgnoreCase("HEARTBEAT");
        }
        if (categoriaSolicitada.equals("EXCLUSION")) {
            return categoriaEvento.equalsIgnoreCase("EXCLUSION");
        }
        return categoriaEvento.equalsIgnoreCase(categoriaSolicitada);
    }

    private void agregarEventos(Map<String, EventoSistema> acumulador, List<EventoSistema> eventosNuevos) {
        for (EventoSistema evento : eventosNuevos) {
            String clave = evento.getFecha() + "|" + evento.getNodoId() + "|" + evento.getCategoria() + "|" + evento.getDescripcion();
            acumulador.putIfAbsent(clave, evento);
        }
    }

    private boolean estaOperativo() {
        return nodoLocalOperativo && nodoLocal().esActivo();
    }

    private boolean esCoordinadorLocal() {
        return idCoordinadorActual == configuracionNodoLocal.getId() || nodoLocal().esCoordinador();
    }

    private NodoHospitalario nodoLocal() {
        return obtenerNodoObligatorio(configuracionNodoLocal.getId());
    }

    private NodoHospitalario obtenerNodoObligatorio(int id) {
        NodoHospitalario nodo = nodos.get(id);
        if (nodo == null) {
            throw new IllegalArgumentException("Nodo no configurado: " + id);
        }
        return nodo;
    }

    private void registrar(String categoria, String descripcion) {
        EventoSistema evento = new EventoSistema(reloj.ahora(), configuracionNodoLocal.getId(), categoria, descripcion);
        registroEventos.registrar(evento);
        LOGGER.info("[{}] {}", categoria, descripcion);
    }

    private boolean esCompatible(String tipoDonante, String tipoReceptor) {
        String donante = normalizar(tipoDonante);
        String receptor = normalizar(tipoReceptor);
        if (donante.isBlank() || receptor.isBlank()) {
            return false;
        }

        String aboDonante = donante.replace("+", "").replace("-", "");
        String aboReceptor = receptor.replace("+", "").replace("-", "");
        boolean aboCompatible = switch (aboDonante) {
            case "O" -> true;
            case "A" -> aboReceptor.equals("A") || aboReceptor.equals("AB");
            case "B" -> aboReceptor.equals("B") || aboReceptor.equals("AB");
            case "AB" -> aboReceptor.equals("AB");
            default -> false;
        };
        boolean rhCompatible = donante.endsWith("-") || receptor.endsWith("+");
        return aboCompatible && rhCompatible;
    }

    private String normalizar(String valor) {
        return valor == null ? "" : valor.trim().toUpperCase(Locale.ROOT);
    }

    private String valorSeguro(String valor) {
        return valor == null ? "" : valor.replace("|", " ");
    }
}
