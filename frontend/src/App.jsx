// Dashboard principal para monitorear y operar la red distribuida de hospitales.
import { useEffect, useMemo, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import {
  Activity,
  AlertTriangle,
  CheckCircle2,
  Clock,
  HeartPulse,
  HelpCircle,
  Hospital,
  Info,
  ListOrdered,
  Lock,
  Network,
  Play,
  Power,
  RadioTower,
  RefreshCw,
  RotateCcw,
  Send,
  Server,
  ShieldCheck,
  Sparkles,
  Stethoscope,
  Unlock,
  Wifi,
  X,
} from 'lucide-react';


const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || `${window.location.protocol}//${window.location.hostname}:8080`;
const REQUEST_TIMEOUT_MS = 9000;
const tiposSangre = ['O-', 'O+', 'A-', 'A+', 'B-', 'B+', 'AB-', 'AB+'];

const estadoVisual = {
  ACTIVO: {
    etiqueta: 'Activo',
    detalle: 'Responde HTTP/TCP y participa en la red.',
  },
  SOSPECHOSO: {
    etiqueta: 'Revisar red',
    detalle: 'No respondio a tiempo. No esta apagado por simulacion.',
  },
  INACTIVO: {
    etiqueta: 'Simulado caido',
    detalle: 'Solo ocurre al presionar Fallar nodo.',
  },
};

function urlApi(path) {
  return path.startsWith('http') ? path : `${API_BASE_URL}${path}`;
}

async function fetchJson(path, options = {}) {
  const controller = new AbortController();
  const timeout = window.setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);

  try {
    const response = await fetch(urlApi(path), {
      headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
      ...options,
      signal: controller.signal,
    });

    const contentType = response.headers.get('content-type') || '';
    const payload = contentType.includes('application/json')
      ? await response.json()
      : await response.text();

    if (!response.ok) {
      const detalle = typeof payload === 'string' ? payload : payload?.message || payload?.error;
      throw new Error(`HTTP ${response.status}${detalle ? ` - ${detalle}` : ''}`);
    }

    return payload;
  } catch (exception) {
    if (exception.name === 'AbortError') {
      throw new Error(`Tiempo agotado conectando con ${urlApi(path)}`);
    }
    throw exception;
  } finally {
    window.clearTimeout(timeout);
  }
}

function formatoHora(valor) {
  if (!valor) return 'sin senal';
  return new Intl.DateTimeFormat('es-EC', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  }).format(new Date(valor));
}

function estadoNodo(nodo) {
  return estadoVisual[nodo?.estado] || {
    etiqueta: nodo?.estado || 'Desconocido',
    detalle: 'Estado no reconocido por el frontend.',
  };
}

function seleccionarObservador(nodos) {
  return nodos.find((nodo) => nodo.id === 1 && nodo.estado !== 'INACTIVO')
    || nodos.find((nodo) => nodo.estado === 'ACTIVO')
    || nodos[0];
}

function App() {
  const [nodos, setNodos] = useState([]);
  const [coordinador, setCoordinador] = useState(null);
  const [logs, setLogs] = useState([]);
  const [instanciasConsul, setInstanciasConsul] = useState([]);
  const [estadoSistema, setEstadoSistema] = useState(null);
  const [estadoCristian, setEstadoCristian] = useState(null);
  const [estadoExclusion, setEstadoExclusion] = useState(null);
  const [error, setError] = useState('');
  const [mensajeAccion, setMensajeAccion] = useState('');
  const [cargando, setCargando] = useState(false);
  const [accion, setAccion] = useState('');
  const [categoriaLog, setCategoriaLog] = useState('TODOS');
  const [grupoLog, setGrupoLog] = useState('PRINCIPAL');
  const [nodoAlgoritmo, setNodoAlgoritmo] = useState(1);
  const [resultadoDonante, setResultadoDonante] = useState(null);
  const [infoAbierta, setInfoAbierta] = useState(false);
  const [confirmacionFallo, setConfirmacionFallo] = useState(null);
  const [ultimaActualizacion, setUltimaActualizacion] = useState(null);
  const [donante, setDonante] = useState({
    nombreDonante: 'Donante Loja',
    tipoSangreDonante: 'O-',
    nombreReceptor: 'Paciente Emergencia',
    tipoSangreReceptor: 'A+',
    urgencia: 'ALTA',
  });

  const nodosActivos = useMemo(
    () => nodos.filter((nodo) => nodo.estado === 'ACTIVO').length,
    [nodos]
  );
  const nodosSospechosos = nodos.filter((nodo) => nodo.estado === 'SOSPECHOSO');
  const nodosInactivos = nodos.filter((nodo) => nodo.estado === 'INACTIVO');
  const nodoCoordinador = coordinador || nodos.find((nodo) => nodo.rol === 'COORDINADOR');
  const logsPorGrupo = useMemo(() => {
    if (grupoLog === 'PRINCIPAL') {
      return logs.filter((log) => ['SIMULACION', 'DONANTES', 'EXCLUSION', 'CRISTIAN', 'INICIO'].includes(log.categoria));
    } else {
      return logs.filter((log) => ['HEARTBEAT', 'BULLY', 'TCP'].includes(log.categoria));
    }
  }, [logs, grupoLog]);

  const categoriasDisponibles = useMemo(() => {
    const cats = Array.from(new Set(logsPorGrupo.map((log) => log.categoria)));
    return ['TODOS', ...cats];
  }, [logsPorGrupo]);

  const logsFiltrados = categoriaLog === 'TODOS'
    ? logsPorGrupo
    : logsPorGrupo.filter((log) => log.categoria === categoriaLog);
  const colaExclusion = estadoExclusion?.colaEspera || [];
  const nodoObjetivo = nodos.find((nodo) => nodo.id === Number(nodoAlgoritmo));

  async function cargarDatos(silencioso = false) {
    if (!silencioso) setCargando(true);
    try {
      const [nodosData, coordinadorData, logsData, consulData] = await Promise.all([
        fetchJson('/api/nodes'),
        fetchJson('/api/nodes/coordinator'),
        fetchJson('/api/logs'),
        fetchJson('/api/nodes/consul'),
      ]);

      const observador = seleccionarObservador(nodosData);
      let estadoData;
      try {
        estadoData = observador
          ? await fetchJson(`http://${observador.host}:${observador.httpPort}/api/estado`)
          : await fetchJson('/api/estado');
      } catch {
        estadoData = await fetchJson('/api/estado');
      }

      setNodos(nodosData);
      setCoordinador(coordinadorData);
      setLogs(logsData);
      setInstanciasConsul(consulData);
      setEstadoSistema(estadoData);
      setEstadoExclusion(estadoData.exclusion);
      setUltimaActualizacion(new Date());

      const objetivo = nodosData.find((nodo) => nodo.id === Number(nodoAlgoritmo)) || nodosData[0];
      if (!nodosData.some((nodo) => nodo.id === Number(nodoAlgoritmo)) && objetivo) {
        setNodoAlgoritmo(objetivo.id);
      }

      if (objetivo) {
        try {
          const cristianDirecto = await fetchJson(`http://${objetivo.host}:${objetivo.httpPort}/api/synchronization/cristian`);
          setEstadoCristian(cristianDirecto);
        } catch {
          setEstadoCristian(estadoData.cristian);
        }
      } else {
        setEstadoCristian(estadoData.cristian);
      }

      setError('');
    } catch (exception) {
      setError(`No se pudo conectar con el API Gateway en ${API_BASE_URL}. ${exception.message}`);
    } finally {
      setCargando(false);
    }
  }

  async function ejecutarAccion(nombre, callback) {
    if (accion) return;
    setAccion(nombre);
    setError('');
    setMensajeAccion('');
    try {
      const respuesta = await callback();
      if (respuesta?.mensaje) {
        setMensajeAccion(respuesta.mensaje);
      }
      await new Promise((resolve) => window.setTimeout(resolve, 700));
      await cargarDatos(true);
    } catch (exception) {
      setError(`Accion fallida: ${exception.message}`);
    } finally {
      setAccion('');
    }
  }

  useEffect(() => {
    cargarDatos();
    const intervalo = window.setInterval(() => cargarDatos(true), 3000);
    return () => window.clearInterval(intervalo);
  }, []);

  const iniciarEleccion = () =>
    ejecutarAccion('eleccion', () =>
      fetchJson('/api/election/start', { method: 'POST' })
    );

  const solicitarFalloNodo = (id) => {
    setConfirmacionFallo(id);
  };

  const fallarNodo = (id) => {
    setConfirmacionFallo(null);
    ejecutarAccion(`fallo-${id}`, () =>
      fetchJson(`/api/simulation/fail/${id}`, { method: 'POST' })
    );
  };

  const fallarCoordinador = () => {
    const id = nodoCoordinador?.id;
    if (!id) {
      setError('No existe coordinador disponible para simular la caida.');
      return;
    }
    solicitarFalloNodo(id);
  };

  const recuperarNodoPorId = (id) =>
    ejecutarAccion(`recuperacion-${id}`, () =>
      fetchJson(`/api/simulation/recover/${id}`, { method: 'POST' })
    );

  const ejecutarCristian = () =>
    ejecutarAccion('cristian', () =>
      fetchJson(`/api/synchronization/cristian/${nodoAlgoritmo}`, { method: 'POST' })
    );

  const solicitarExclusion = () =>
    ejecutarAccion('exclusion-solicitar', () =>
      fetchJson(`/api/exclusion/request/${nodoAlgoritmo}`, { method: 'POST' })
    );

  const liberarExclusion = () =>
    ejecutarAccion('exclusion-liberar', () =>
      fetchJson(`/api/exclusion/release/${nodoAlgoritmo}`, { method: 'POST' })
    );

  function validarDonante() {
    if (donante.nombreDonante.trim().length < 3) {
      throw new Error('Ingrese un nombre de donante con al menos 3 caracteres.');
    }
    if (donante.nombreReceptor.trim().length < 3) {
      throw new Error('Ingrese un nombre de receptor con al menos 3 caracteres.');
    }
    if (!tiposSangre.includes(donante.tipoSangreDonante) || !tiposSangre.includes(donante.tipoSangreReceptor)) {
      throw new Error('Seleccione tipos de sangre validos.');
    }
  }

  const enviarConsultaDonante = (event) => {
    event.preventDefault();
    ejecutarAccion('donante', async () => {
      validarDonante();

      const objetivo = nodos.find((n) => n.id === Number(nodoAlgoritmo));
      let url = '/api/donors/compatibility';
      if (objetivo) {
        url = `http://${objetivo.host}:${objetivo.httpPort}/api/donors/compatibility`;
      }

      const resultado = await fetchJson(url, {
        method: 'POST',
        body: JSON.stringify({
          ...donante,
          nombreDonante: donante.nombreDonante.trim(),
          nombreReceptor: donante.nombreReceptor.trim(),
        }),
      });
      setResultadoDonante(resultado);
      return { mensaje: resultado.mensaje };
    });
  };

  return (
    <main className="app-shell">
      <section className="landing-card" id="panel">
        <nav className="topbar">
          <div className="brand">
            <span className="brand-mark"><HeartPulse size={18} /></span>
            <strong>RedHospital</strong>
          </div>
          <div className="nav-links">
            <a href="#panel">Panel</a>
            <a href="#nodos">Nodos</a>
            <a href="#algoritmos">Algoritmos</a>
            <a href="#logs">Logs</a>
          </div>
          <button className="pill-button light" onClick={() => setInfoAbierta(true)}>
            <Info size={17} />
            Informacion
          </button>
        </nav>

        <div className="hero-grid">
          <div className="hero-copy">
            <span className="hero-badge"><Sparkles size={15} /> Fast monitoring</span>
            <h1>Red Smart Hospital</h1>
            <p>
              Dashboard para probar una red distribuida de hospitales con sockets TCP,
              Consul y los algoritmos Bully, Cristian y Exclusion Mutua.
            </p>
            <div className="hero-actions">
              <button className="pill-button primary" onClick={() => cargarDatos()} disabled={cargando}>
                <RefreshCw size={18} />
                {cargando ? 'Actualizando...' : 'Actualizar red'}
              </button>
            </div>
          </div>
        </div>

        <div className="feature-strip">
          <MiniFeature icono={<ShieldCheck size={18} />} titulo="Bully" texto="El mayor ID activo lidera." />
          <MiniFeature icono={<Clock size={18} />} titulo="Cristian" texto="Calcula hora ajustada." />
          <MiniFeature icono={<Lock size={18} />} titulo="Exclusion Mutua" texto="Una cola FIFO protege el recurso." />
        </div>
      </section>

      {error && (
        <motion.div className="alerta" initial={{ opacity: 0, y: -8 }} animate={{ opacity: 1, y: 0 }}>
          <AlertTriangle size={18} />
          <span>{error}</span>
        </motion.div>
      )}

      {mensajeAccion && !error && (
        <motion.div className="aviso" initial={{ opacity: 0, y: -8 }} animate={{ opacity: 1, y: 0 }}>
          <CheckCircle2 size={18} />
          <span>{mensajeAccion}</span>
        </motion.div>
      )}

      <section className="resumen-grid">
        <PanelMetrica icono={<Server size={22} />} etiqueta="Nodos activos" valor={`${nodosActivos}/${nodos.length || 4}`} tono="azul" />
        <PanelMetrica icono={<ShieldCheck size={22} />} etiqueta="Coordinador" valor={nodoCoordinador ? `Nodo ${nodoCoordinador.id}` : 'Pendiente'} tono="verde" />
        <PanelMetrica icono={<Wifi size={22} />} etiqueta="Consul" valor={`${instanciasConsul.length} instancias`} tono="gris" linkUrl="http://localhost:8500" />
        <PanelMetrica
          icono={<Hospital size={22} />}
          etiqueta="Estado de red"
          valor={nodosInactivos.length ? `${nodosInactivos.length} simulado caido` : nodosSospechosos.length ? `${nodosSospechosos.length} revisar red` : 'Estable'}
          tono="naranja"
        />
      </section>

      <section className="acciones">
        <button onClick={iniciarEleccion} disabled={Boolean(accion)} className="pill-button primary">
          <Play size={18} />
          Iniciar eleccion
        </button>
        <button onClick={fallarCoordinador} disabled={Boolean(accion)} className="pill-button danger">
          <Power size={18} />
          Simular caida del coordinador
        </button>
        <span className="last-update">
          Ultima lectura: {ultimaActualizacion ? formatoHora(ultimaActualizacion) : 'pendiente'}
        </span>
      </section>

      <section className="panel algoritmos-panel" id="algoritmos">
        <PanelHeader
          eyebrow="Control de concurrencia"
          titulo="Algoritmos: Bully, Cristian y Exclusion Mutua"
          texto="Estos son los tres algoritmos distribuidos implementados en la red hospitalaria."
          icono={<Network size={20} />}
        />

        <div className="algoritmos-grid">
          <div className="algoritmo-bloque">
            <span className="algoritmo-label">Bully</span>
            <strong>{nodoCoordinador ? `Coordinador actual: Nodo ${nodoCoordinador.id}` : 'Coordinador pendiente'}</strong>
            <small>Algoritmo de eleccion de coordinador</small>
            <p>Bully selecciona como coordinador al nodo activo con ID mas alto. Solo cambia cuando se inicia una eleccion o se simula una caida.</p>
          </div>

          <div className="algoritmo-bloque">
            <span className="algoritmo-label">Cristian</span>
            <strong>RTT {estadoCristian?.rttMs ?? 0} ms</strong>
            <small>Local: {formatoHora(estadoCristian?.horaLocal)}</small>
            <small>Coordinador: {formatoHora(estadoCristian?.horaCoordinador)}</small>
            <small>Ajustada: {formatoHora(estadoCristian?.horaAjustada)}</small>
            <small>
              Ajuste real: {estadoCristian?.ajusteSistemaHabilitado
                ? (estadoCristian?.relojSistemaAjustado ? 'reloj del sistema ajustado' : 'habilitado, esperando permisos')
                : 'solo calculado'}
            </small>
            <p>Cristian toma la hora del coordinador, calcula latencia y puede ajustar el reloj real del sistema si se inicia con permisos.</p>
          </div>

          <div className="algoritmo-bloque">
            <span className="algoritmo-label">Exclusion Mutua</span>
            <strong>{estadoExclusion?.estadoLocal || 'LIBRE'}</strong>
            <small>Recurso: {estadoExclusion?.recurso || 'directorio-donantes'}</small>
            <small>
              Seccion critica: {estadoExclusion?.nodoEnSeccionCritica
                ? `Nodo ${estadoExclusion.nodoEnSeccionCritica}`
                : 'libre'}
            </small>
            <small>Cola FIFO: {colaExclusion.length ? colaExclusion.map((id) => `Nodo ${id}`).join(', ') : 'vacia'}</small>
            <p>La exclusion mutua permite que solo un nodo entre al recurso critico; los demas esperan en una cola FIFO.</p>
          </div>

          <div className="algoritmo-controles">
            <label>
              Nodo de prueba
              <select value={nodoAlgoritmo} onChange={(event) => setNodoAlgoritmo(Number(event.target.value))}>
                {nodos.map((nodo) => (
                  <option key={nodo.id} value={nodo.id}>
                    Nodo {nodo.id} - {nodo.nombreHospital}
                  </option>
                ))}
              </select>
            </label>
            <div className="algoritmo-botones">
              <button onClick={ejecutarCristian} disabled={Boolean(accion) || !nodoObjetivo} className="pill-button light">
                <Clock size={18} />
                Sincronizar
              </button>
              <button onClick={solicitarExclusion} disabled={Boolean(accion) || !nodoObjetivo} className="pill-button primary">
                <Lock size={18} />
                Solicitar
              </button>
              <button onClick={liberarExclusion} disabled={Boolean(accion) || !nodoObjetivo} className="pill-button danger">
                <Unlock size={18} />
                Liberar
              </button>
            </div>
            <div className="cola-fifo">
              <ListOrdered size={17} />
              <span>{colaExclusion.length ? colaExclusion.join(' -> ') : 'Sin espera'}</span>
            </div>
          </div>
        </div>
      </section>

      <section className="nodos-section" id="nodos">
        <PanelHeader
          eyebrow="Vista de infraestructura"
          titulo="Nodos hospitalarios"
          texto="INACTIVO solo aparece cuando alguien presiona Fallar. SOSPECHOSO significa revisar IP, firewall o cableado."
          icono={<RadioTower size={20} />}
        />
        <div className="nodos-grid">
          <AnimatePresence>
            {nodos.map((nodo) => (
              <NodoCard
                key={nodo.id}
              nodo={nodo}
              esCoordinador={nodo.rol === 'COORDINADOR'}
              accion={accion}
              onFallar={() => solicitarFalloNodo(nodo.id)}
              onRecuperar={() => recuperarNodoPorId(nodo.id)}
            />
            ))}
          </AnimatePresence>
        </div>
      </section>

      <section className="contenido-grid">
        <section className="panel panel-logs" id="logs">
          <PanelHeader
            eyebrow="Eventos consolidados"
            titulo="Logs del sistema"
            texto="Muestra elecciones, heartbeats, simulaciones, sincronizacion y exclusion mutua."
            icono={<Activity size={20} />}
          />
          <div className="tabs-log">
            <button
              className={grupoLog === 'PRINCIPAL' ? 'activo' : ''}
              onClick={() => {
                setGrupoLog('PRINCIPAL');
                setCategoriaLog('TODOS');
              }}
            >
              Principal (Acciones y Fallos)
            </button>
            <button
              className={grupoLog === 'DIAGNOSTICO' ? 'activo' : ''}
              onClick={() => {
                setGrupoLog('DIAGNOSTICO');
                setCategoriaLog('TODOS');
              }}
            >
              Diagnóstico de Red (Latidos, Bully)
            </button>
          </div>

          <div className="filtros-log">
            {categoriasDisponibles.map((categoria) => (
              <button
                key={categoria}
                className={categoriaLog === categoria ? 'activo' : ''}
                onClick={() => setCategoriaLog(categoria)}
              >
                {categoria}
              </button>
            ))}
          </div>
          <div className="log-lista">
            {logsFiltrados.length === 0 && <p className="texto-muted">Sin eventos disponibles.</p>}
            <AnimatePresence>
              {logsFiltrados.slice(0, 20).map((log, index) => (
                <motion.article
                  key={`${log.fecha}-${index}`}
                  className={`log-item log-${log.categoria.toLowerCase()}`}
                  initial={{ opacity: 0, x: -8 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0 }}
                >
                  <span className="log-categoria">{log.categoria}</span>
                  <p>{log.descripcion}</p>
                  <time>{formatoHora(log.fecha)} - nodo {log.nodoId}</time>
                </motion.article>
              ))}
            </AnimatePresence>
          </div>
        </section>

        <section className="panel panel-donantes">
          <PanelHeader
            eyebrow="Banco de sangre"
            titulo="Compatibilidad de donante"
            texto="Simula una consulta clinica y registra el resultado en la red distribuida."
            icono={<Stethoscope size={20} />}
          />

          <form className="formulario" onSubmit={enviarConsultaDonante}>
            <label>
              Donante
              <input
                value={donante.nombreDonante}
                onChange={(event) => setDonante({ ...donante, nombreDonante: event.target.value })}
                placeholder="Nombre del donante"
              />
            </label>
            <label>
              Tipo donante
              <select
                value={donante.tipoSangreDonante}
                onChange={(event) => setDonante({ ...donante, tipoSangreDonante: event.target.value })}
              >
                {tiposSangre.map((tipo) => (
                  <option key={tipo}>{tipo}</option>
                ))}
              </select>
            </label>
            <label>
              Receptor
              <input
                value={donante.nombreReceptor}
                onChange={(event) => setDonante({ ...donante, nombreReceptor: event.target.value })}
                placeholder="Nombre del receptor"
              />
            </label>
            <label>
              Tipo receptor
              <select
                value={donante.tipoSangreReceptor}
                onChange={(event) => setDonante({ ...donante, tipoSangreReceptor: event.target.value })}
              >
                {tiposSangre.map((tipo) => (
                  <option key={tipo}>{tipo}</option>
                ))}
              </select>
            </label>
            <label>
              Urgencia
              <select value={donante.urgencia} onChange={(event) => setDonante({ ...donante, urgencia: event.target.value })}>
                <option>BAJA</option>
                <option>MEDIA</option>
                <option>ALTA</option>
                <option>CRITICA</option>
              </select>
            </label>

            <button className="pill-button primary full" disabled={accion === 'donante'}>
              <Send size={18} />
              Solicitar organo
            </button>
          </form>

          {resultadoDonante && (
            <motion.div
              className={`resultado ${resultadoDonante.compatible ? 'ok' : 'warning'}`}
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
            >
              <strong>{resultadoDonante.compatible ? 'Compatible' : 'No compatible'}</strong>
              <span>{resultadoDonante.mensaje}</span>
              <small>Coordinador consultado: {resultadoDonante.coordinadorConsultado}</small>
            </motion.div>
          )}
        </section>
      </section>

      <InfoModal abierto={infoAbierta} onCerrar={() => setInfoAbierta(false)} apiBaseUrl={API_BASE_URL} />
      <ConfirmarFalloModal
        nodeId={confirmacionFallo}
        onCancelar={() => setConfirmacionFallo(null)}
        onConfirmar={() => fallarNodo(confirmacionFallo)}
      />
    </main>
  );
}

function MiniFeature({ icono, titulo, texto }) {
  return (
    <article>
      <span>{icono}</span>
      <div>
        <strong>{titulo}</strong>
        <small>{texto}</small>
      </div>
    </article>
  );
}

function PanelHeader({ eyebrow, titulo, texto, icono }) {
  return (
    <div className="panel-header">
      <div>
        <p className="eyebrow">{eyebrow}</p>
        <h2>{titulo}</h2>
        {texto && <span>{texto}</span>}
      </div>
      {icono}
    </div>
  );
}

function PanelMetrica({ icono, etiqueta, valor, tono, linkUrl }) {
  const contenido = (
    <>
      <div className="metrica-icono">{icono}</div>
      <div>
        <span>{etiqueta}</span>
        <strong>{valor}</strong>
      </div>
    </>
  );

  return (
    <motion.article className={`metrica ${tono}`} initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }}>
      {linkUrl ? (
        <a href={linkUrl} target="_blank" rel="noopener noreferrer" style={{ display: 'contents', color: 'inherit', textDecoration: 'none' }}>
          {contenido}
        </a>
      ) : (
        contenido
      )}
    </motion.article>
  );
}

function NodoCard({ nodo, esCoordinador, accion, onFallar, onRecuperar }) {
  const infoEstado = estadoNodo(nodo);
  const activo = nodo.estado === 'ACTIVO';
  const inactivo = nodo.estado === 'INACTIVO';
  return (
    <motion.article
      className={`nodo-card ${nodo.estado.toLowerCase()} ${esCoordinador ? 'coordinador' : ''}`}
      layout
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0 }}
    >
      <div className="nodo-card-top">
        <div>
          <p className="eyebrow">Nodo {nodo.id}</p>
          <h3>{nodo.nombreHospital}</h3>
        </div>
        <span className={`estado ${nodo.estado.toLowerCase()}`}>{infoEstado.etiqueta}</span>
      </div>
      <p className="nodo-explicacion">{infoEstado.detalle}</p>
      <div className="nodo-detalles">
        <span>HTTP {nodo.httpPort}</span>
        <span>TCP {nodo.tcpPort}</span>
        <span>{nodo.host}</span>
      </div>
      <div className="nodo-footer">
        <strong>{esCoordinador && activo ? 'COORDINADOR' : nodo.rol}</strong>
        <time>{formatoHora(nodo.ultimaSenal)}</time>
      </div>
      <div className="nodo-actions">
        <button className="mini-button danger" onClick={onFallar} disabled={inactivo || Boolean(accion)}>
          <Power size={15} />
          Fallar
        </button>
        <button className="mini-button ok" onClick={onRecuperar} disabled={!inactivo || Boolean(accion)}>
          <RotateCcw size={15} />
          Recuperar
        </button>
      </div>
    </motion.article>
  );
}

function InfoModal({ abierto, onCerrar, apiBaseUrl }) {
  return (
    <AnimatePresence>
      {abierto && (
        <motion.div className="modal-backdrop" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
          <motion.section
            className="info-modal"
            initial={{ opacity: 0, y: 24, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 18, scale: 0.98 }}
          >
            <button className="modal-close" onClick={onCerrar} aria-label="Cerrar informacion">
              <X size={18} />
            </button>
            <p className="eyebrow">Hola, este es nuestro proyecto</p>
            <h2>Red distribuida de hospitales</h2>
            <p>
              Este sistema representa cuatro hospitales conectados en una red local.
              Cada hospital es un nodo Spring Boot independiente; se comunican por TCP
              para ejecutar algoritmos distribuidos y usan Consul para descubrimiento.
            </p>
            <div className="modal-grid">
              <MiniFeature icono={<RadioTower size={18} />} titulo="Bully" texto="Elige automaticamente al coordinador con mayor ID activo." />
              <MiniFeature icono={<Clock size={18} />} titulo="Cristian" texto="Consulta al coordinador y puede ajustar el reloj real." />
              <MiniFeature icono={<Lock size={18} />} titulo="Exclusion Mutua" texto="Controla una seccion critica con cola FIFO centralizada." />
              <MiniFeature icono={<HeartPulse size={18} />} titulo="Donantes" texto="Simula compatibilidad de sangre y registra la consulta." />
            </div>
            <div className="modal-steps">
              <strong>Que se puede demostrar</strong>
              <ol>
                <li>Ver el coordinador actual elegido por Bully.</li>
                <li>Simular la caida de un nodo y recuperarlo.</li>
                <li>Ejecutar Cristian para calcular y aplicar la hora ajustada.</li>
                <li>Solicitar y liberar la seccion critica con Exclusion Mutua.</li>
              </ol>
              <small>API Gateway detectado: {apiBaseUrl}</small>
            </div>
          </motion.section>
        </motion.div>
      )}
    </AnimatePresence>
  );
}

function ConfirmarFalloModal({ nodeId, onCancelar, onConfirmar }) {
  return (
    <AnimatePresence>
      {nodeId && (
        <motion.div className="modal-backdrop" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
          <motion.section
            className="info-modal confirm-modal"
            initial={{ opacity: 0, y: 24, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 18, scale: 0.98 }}
          >
            <button className="modal-close" onClick={onCancelar} aria-label="Cancelar simulacion de fallo">
              <X size={18} />
            </button>
            <p className="eyebrow">Confirmacion del sistema</p>
            <h2>Simular caida del nodo {nodeId}</h2>
            <p>
              Esta accion marcara el nodo {nodeId} como INACTIVO dentro de la simulacion.
              No se apaga tu computadora; solo se prueba como responde la red distribuida.
            </p>
            <div className="modal-actions">
              <button className="pill-button ghost" onClick={onCancelar}>
                Cancelar
              </button>
              <button className="pill-button danger" onClick={onConfirmar}>
                Confirmar caida
              </button>
            </div>
          </motion.section>
        </motion.div>
      )}
    </AnimatePresence>
  );
}

export default App;
