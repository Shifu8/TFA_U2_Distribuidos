// Dashboard principal para monitorear y operar la red distribuida de hospitales.
import { useEffect, useMemo, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Activity,
  AlertTriangle,
  Clock,
  HeartPulse,
  Hospital,
  Network,
  Play,
  Power,
  RadioTower,
  RefreshCw,
  Send,
  Server,
  ShieldCheck,
  Stethoscope,
  Wifi,
} from 'lucide-react';
import heroDoctor from './assets/doctor-network-hero.png';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
const tiposSangre = ['O-', 'O+', 'A-', 'A+', 'B-', 'B+', 'AB-', 'AB+'];

async function fetchJson(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }

  return response.json();
}

function formatoHora(valor) {
  if (!valor) return 'sin senal';
  return new Intl.DateTimeFormat('es-EC', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  }).format(new Date(valor));
}

function App() {
  const [nodos, setNodos] = useState([]);
  const [coordinador, setCoordinador] = useState(null);
  const [logs, setLogs] = useState([]);
  const [instanciasConsul, setInstanciasConsul] = useState([]);
  const [error, setError] = useState('');
  const [mensajeAccion, setMensajeAccion] = useState('');
  const [cargando, setCargando] = useState(false);
  const [accion, setAccion] = useState('');
  const [categoriaLog, setCategoriaLog] = useState('TODOS');
  const [resultadoDonante, setResultadoDonante] = useState(null);
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

  const nodoCoordinador = coordinador || nodos.find((nodo) => nodo.rol === 'COORDINADOR');
  const nodosInactivos = nodos.filter((nodo) => nodo.estado === 'INACTIVO');
  const categorias = ['TODOS', ...Array.from(new Set(logs.map((log) => log.categoria)))];
  const logsFiltrados = categoriaLog === 'TODOS'
    ? logs
    : logs.filter((log) => log.categoria === categoriaLog);

  async function cargarDatos(silencioso = false) {
    if (!silencioso) setCargando(true);
    try {
      const [nodosData, coordinadorData, logsData, consulData] = await Promise.all([
        fetchJson('/api/nodes'),
        fetchJson('/api/nodes/coordinator'),
        fetchJson('/api/logs'),
        fetchJson('/api/nodes/consul'),
      ]);
      setNodos(nodosData);
      setCoordinador(coordinadorData);
      setLogs(logsData);
      setInstanciasConsul(consulData);
      setError('');
    } catch (exception) {
      setError(`No se pudo conectar con el API Gateway en ${API_BASE_URL}`);
    } finally {
      setCargando(false);
    }
  }

  async function ejecutarAccion(nombre, callback) {
    setAccion(nombre);
    setError('');
    setMensajeAccion('');
    try {
      const respuesta = await callback();
      if (respuesta?.mensaje) {
        setMensajeAccion(respuesta.mensaje);
      }
      await new Promise((resolve) => window.setTimeout(resolve, 900));
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

  const fallarNodo = (id) =>
    ejecutarAccion(`fallo-${id}`, () =>
      fetchJson(`/api/simulation/fail/${id}`, { method: 'POST' })
    );

  const fallarCoordinador = () =>
    ejecutarAccion('fallo-coordinador', () => {
      const id = nodoCoordinador?.id;
      if (!id) throw new Error('No existe coordinador disponible');
      return fetchJson(`/api/simulation/fail/${id}`, { method: 'POST' });
    });

  const enviarConsultaDonante = (event) => {
    event.preventDefault();
    ejecutarAccion('donante', async () => {
      const resultado = await fetchJson('/api/donors/compatibility', {
        method: 'POST',
        body: JSON.stringify(donante),
      });
      setResultadoDonante(resultado);
      return { mensaje: resultado.mensaje };
    });
  };

  return (
    <main className="app-shell">
      <section className="hero-operativo">
        <div className="hero-copy">
          <p className="eyebrow">Sistema distribuido hospitalario</p>
          <h1>Red de Hospitales</h1>
          <div className="hero-status">
            <span><RadioTower size={16} /> Heartbeat 2s</span>
            <span><Network size={16} /> Bully activo</span>
            <span><Clock size={16} /> Cristian sync</span>
            <span><Stethoscope size={16} /> Guardia distribuida</span>
          </div>
        </div>
        <div className="hero-media">
          <img src={heroDoctor} alt="Doctor monitoreando una red hospitalaria distribuida" />
        </div>
        <button className="icon-button secondary hero-refresh" onClick={() => cargarDatos()} disabled={cargando}>
          <RefreshCw size={18} />
          Actualizar
        </button>
      </section>

      {error && (
        <motion.div className="alerta" initial={{ opacity: 0, y: -8 }} animate={{ opacity: 1, y: 0 }}>
          <AlertTriangle size={18} />
          <span>{error}</span>
        </motion.div>
      )}

      {mensajeAccion && !error && (
        <motion.div className="aviso" initial={{ opacity: 0, y: -8 }} animate={{ opacity: 1, y: 0 }}>
          <ShieldCheck size={18} />
          <span>{mensajeAccion}</span>
        </motion.div>
      )}

      <section className="resumen-grid">
        <PanelMetrica
          icono={<Server size={22} />}
          etiqueta="Nodos activos"
          valor={`${nodosActivos}/${nodos.length || 4}`}
          tono="azul"
        />
        <PanelMetrica
          icono={<ShieldCheck size={22} />}
          etiqueta="Coordinador"
          valor={nodoCoordinador ? `Nodo ${nodoCoordinador.id}` : 'Pendiente'}
          tono="verde"
        />
        <PanelMetrica
          icono={<Wifi size={22} />}
          etiqueta="Consul"
          valor={`${instanciasConsul.length} instancias`}
          tono="gris"
        />
        <PanelMetrica
          icono={<Hospital size={22} />}
          etiqueta="Hospitales"
          valor={nodosInactivos.length ? `${nodosInactivos.length} en alerta` : 'Estables'}
          tono="naranja"
        />
      </section>

      <section className="acciones">
        <button onClick={iniciarEleccion} disabled={Boolean(accion)} className="icon-button primary">
          <Play size={18} />
          Iniciar eleccion
        </button>
        <button onClick={fallarCoordinador} disabled={Boolean(accion)} className="icon-button danger">
          <Power size={18} />
          Fallar coordinador
        </button>
      </section>

      <section className="nodos-grid">
        <AnimatePresence>
          {nodos.map((nodo) => (
            <NodoCard
              key={nodo.id}
              nodo={nodo}
              esCoordinador={nodo.rol === 'COORDINADOR'}
              accion={accion}
              onFallar={() => fallarNodo(nodo.id)}
            />
          ))}
        </AnimatePresence>
      </section>

      <section className="contenido-grid">
        <section className="panel panel-logs">
          <div className="panel-header">
            <div>
              <p className="eyebrow">Eventos consolidados</p>
              <h2>Logs del sistema</h2>
            </div>
            <Activity size={20} />
          </div>
          <div className="filtros-log">
            {categorias.map((categoria) => (
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
              {logsFiltrados.slice(0, 18).map((log, index) => (
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
          <div className="panel-header">
            <div>
              <p className="eyebrow">Banco de sangre</p>
              <h2>Compatibilidad de donante</h2>
            </div>
            <HeartPulse size={20} />
          </div>

          <form className="formulario" onSubmit={enviarConsultaDonante}>
            <label>
              Donante
              <input
                value={donante.nombreDonante}
                onChange={(event) => setDonante({ ...donante, nombreDonante: event.target.value })}
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
              <select
                value={donante.urgencia}
                onChange={(event) => setDonante({ ...donante, urgencia: event.target.value })}
              >
                <option>BAJA</option>
                <option>MEDIA</option>
                <option>ALTA</option>
                <option>CRITICA</option>
              </select>
            </label>

            <button className="icon-button primary full" disabled={accion === 'donante'}>
              <Send size={18} />
              Consultar
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
              <small>Coordinador: {resultadoDonante.coordinadorConsultado}</small>
            </motion.div>
          )}
        </section>
      </section>
    </main>
  );
}

function PanelMetrica({ icono, etiqueta, valor, tono }) {
  return (
    <motion.article className={`metrica ${tono}`} initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }}>
      <div className="metrica-icono">{icono}</div>
      <div>
        <span>{etiqueta}</span>
        <strong>{valor}</strong>
      </div>
    </motion.article>
  );
}

function NodoCard({ nodo, esCoordinador, accion, onFallar }) {
  const activo = nodo.estado === 'ACTIVO';
  return (
    <motion.article
      className={`nodo-card ${activo ? 'activo' : 'inactivo'} ${esCoordinador ? 'coordinador' : ''}`}
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
        <span className={`estado ${nodo.estado.toLowerCase()}`}>{nodo.estado}</span>
      </div>
      <div className="nodo-detalles">
        <span>HTTP {nodo.httpPort}</span>
        <span>TCP {nodo.tcpPort}</span>
        <span>{nodo.host}</span>
      </div>
      <div className="nodo-footer">
        <strong>{nodo.rol}</strong>
        <time>{formatoHora(nodo.ultimaSenal)}</time>
      </div>
      <div className="nodo-actions">
        <button className="mini-button danger" onClick={onFallar} disabled={!activo || Boolean(accion)}>
          <Power size={15} />
          Fallar
        </button>

      </div>
    </motion.article>
  );
}

export default App;
