#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="$ROOT_DIR/logs"
NODE_ID="${1:-}"
AJUSTAR_RELOJ_SISTEMA="${AJUSTAR_RELOJ_SISTEMA:-true}"

if [[ -z "$NODE_ID" ]]; then
  echo "Uso: ./scripts/iniciar-nodo.sh <nodeId>"
  echo "Ejemplo: ./scripts/iniciar-nodo.sh 1"
  exit 1
fi

mkdir -p "$LOG_DIR"

if [[ "$AJUSTAR_RELOJ_SISTEMA" == "true" ]]; then
  echo "Cristian ajustara la hora real del sistema en este nodo. Validando permisos sudo..."
  sudo -v
fi

command_exists() {
  command -v "$1" >/dev/null 2>&1
}

needs_backend_build() {
  local hospital_jar="$ROOT_DIR/backend/hospital-service/target/hospital-service.jar"
  local gateway_jar="$ROOT_DIR/backend/api-gateway/target/api-gateway.jar"

  if [[ ! -f "$hospital_jar" || ! -f "$gateway_jar" ]]; then
    return 0
  fi

  if [[ "$ROOT_DIR/pom.xml" -nt "$hospital_jar" || "$ROOT_DIR/pom.xml" -nt "$gateway_jar" ]]; then
    return 0
  fi

  if find "$ROOT_DIR/backend" -type f \( -name '*.java' -o -name '*.yml' -o -name 'pom.xml' \) \
      \( -newer "$hospital_jar" -o -newer "$gateway_jar" \) -print -quit | grep -q .; then
    return 0
  fi

  return 1
}

node_config_with_python() {
  local python_cmd="$1"
  "$python_cmd" - "$ROOT_DIR/nodes.json" "$NODE_ID" <<'PY'
import json
import sys

path = sys.argv[1]
node_id = int(sys.argv[2])

with open(path, "r", encoding="utf-8") as fh:
    data = json.load(fh)

nodes = data if isinstance(data, list) else data.get("nodos", [])
for node in nodes:
    if int(node.get("id")) == node_id:
        host = node.get("host", "localhost")
        tcp = node.get("tcpPort", node.get("port", 9000 + node_id))
        http = node.get("httpPort", node.get("apiPort", 8080 + node_id))
        print(f"{host} {tcp} {http}")
        sys.exit(0)

raise SystemExit(f"No existe el nodo {node_id} en nodes.json")
PY
}

node_config_with_node() {
  node - "$ROOT_DIR/nodes.json" "$NODE_ID" <<'JS'
const fs = require('fs');
const path = process.argv[2];
const nodeId = Number(process.argv[3]);
const data = JSON.parse(fs.readFileSync(path, 'utf8'));
const nodes = Array.isArray(data) ? data : data.nodos || [];
const nodeConfig = nodes.find((node) => Number(node.id) === nodeId);
if (!nodeConfig) {
  console.error(`No existe el nodo ${nodeId} en nodes.json`);
  process.exit(1);
}
const host = nodeConfig.host || 'localhost';
const tcp = nodeConfig.tcpPort || nodeConfig.port || (9000 + nodeId);
const http = nodeConfig.httpPort || nodeConfig.apiPort || (8080 + nodeId);
console.log(`${host} ${tcp} ${http}`);
JS
}

read_node_config() {
  if command_exists python3; then
    node_config_with_python python3
    return
  fi
  if command_exists python; then
    node_config_with_python python
    return
  fi
  if command_exists node; then
    node_config_with_node
    return
  fi

  echo "Se requiere python3, python o node para leer nodes.json desde Bash." >&2
  exit 1
}

read NODE_HOST NODE_TCP_PORT NODE_HTTP_PORT < <(read_node_config)

CONSUL_HOST="${CONSUL_HOST:-}"
if [[ -z "$CONSUL_HOST" ]]; then
  CONSUL_HOST="$(NODE_ID=1 read_node_config | awk '{print $1}')"
fi

es_principal=false
if [[ "$NODE_ID" == "1" ]]; then
  es_principal=true
fi

linea() {
  echo "============================================================"
}

mostrar_advertencias_red() {
  if [[ "$NODE_HOST" == "localhost" || "$NODE_HOST" == "127.0.0.1" ]]; then
    echo "ADVERTENCIA: El nodo $NODE_ID esta configurado con host '$NODE_HOST'."
    echo "Para 4 PCs reales, cambia nodes.json y coloca la IP LAN de cada computadora."
    echo ""
    return
  fi

  if command_exists hostname; then
    local ips_locales
    ips_locales="$(hostname -I 2>/dev/null || true)"
    if [[ -n "$ips_locales" && " $ips_locales " != *" $NODE_HOST "* ]]; then
      echo "ADVERTENCIA: La IP $NODE_HOST del nodo $NODE_ID no aparece entre las IPs locales:"
      echo "$ips_locales"
      echo "Si esta es la PC del nodo $NODE_ID, revisa nodes.json antes de la defensa."
      echo ""
    fi
  fi
}

mostrar_plan_arranque() {
  linea
  echo "Arranque de Red de Hospitales - Nodo $NODE_ID"
  linea
  if [[ "$es_principal" == true ]]; then
    echo "Esta PC es la PRINCIPAL."
    echo "Se iniciara: Consul + API Gateway + Frontend + Nodo 1."
  else
    echo "Esta PC es un nodo hospitalario secundario."
    echo "Se iniciara solo el servicio del nodo $NODE_ID."
    echo "El frontend, Consul y Gateway viven en la PC 1: $CONSUL_HOST."
  fi
  echo "Host del nodo: $NODE_HOST"
  echo "HTTP del nodo: $NODE_HTTP_PORT"
  echo "TCP del nodo:  $NODE_TCP_PORT"
  echo "Consul host:   $CONSUL_HOST"
  echo "Ajuste reloj:  $AJUSTAR_RELOJ_SISTEMA"
  linea
  echo ""
}

start_background() {
  local name="$1"
  shift
  local pid_file="$LOG_DIR/$name.pid"

  if [[ -f "$pid_file" ]] && kill -0 "$(cat "$pid_file")" >/dev/null 2>&1; then
    echo "$name ya esta ejecutandose con PID $(cat "$pid_file")"
    return
  fi

  rm -f "$pid_file"
  echo "Iniciando $name..."
  nohup "$@" > "$LOG_DIR/$name.out.log" 2> "$LOG_DIR/$name.err.log" &
  echo $! > "$pid_file"

  sleep 1
  if ! kill -0 "$(cat "$pid_file")" >/dev/null 2>&1; then
    echo "ERROR: $name termino durante el arranque."
    mostrar_error_arranque "$name"
    rm -f "$pid_file"
    exit 1
  fi
}

mostrar_error_arranque() {
  local name="$1"
  local error_log="$LOG_DIR/$name.err.log"
  local output_log="$LOG_DIR/$name.out.log"

  echo "Ultimas lineas de $name:"
  if [[ -s "$error_log" ]]; then
    tail -n 30 "$error_log"
  elif [[ -s "$output_log" ]]; then
    tail -n 30 "$output_log"
  else
    echo "No se genero informacion en los logs."
  fi
}

wait_for_port() {
  local name="$1"
  local host="$2"
  local port="$3"
  local timeout_seconds="${4:-30}"
  local elapsed=0

  echo "Esperando $name en $host:$port..."
  while (( elapsed < timeout_seconds )); do
    if (echo > "/dev/tcp/$host/$port") >/dev/null 2>&1; then
      echo "$name listo en $host:$port"
      return
    fi
    sleep 1
    ((elapsed += 1))
  done

  echo "ERROR: $name no abrio el puerto $port despues de ${timeout_seconds}s."
  mostrar_error_arranque "$name"
  exit 1
}

cd "$ROOT_DIR"

mostrar_plan_arranque
mostrar_advertencias_red

JAVA_CMD="$(command -v java || true)"
if [[ -z "$JAVA_CMD" ]]; then
  echo "ERROR: Java no esta instalado o no esta en PATH. Instala Java 21 antes de iniciar los nodos."
  exit 1
fi

if needs_backend_build; then
  echo "Compilando backend con Maven..."
  mvn clean package -DskipTests
fi

if [[ "$es_principal" == true ]]; then
  if ! command_exists consul; then
    echo "Consul no esta instalado o no esta en PATH. Instala Consul antes de iniciar la PC principal."
    exit 1
  fi

  start_background "consul" consul agent -dev -client=0.0.0.0
  wait_for_port "consul" "127.0.0.1" 8500 20

  start_background "gateway" env CONSUL_HOST="$CONSUL_HOST" GATEWAY_HOST="$NODE_HOST" \
    "$JAVA_CMD" -jar "$ROOT_DIR/backend/api-gateway/target/api-gateway.jar" --server.port=8080
  wait_for_port "gateway" "127.0.0.1" 8080 60

  if command_exists npm; then
    if [[ ! -d "$ROOT_DIR/frontend/node_modules" ]]; then
      echo "Instalando dependencias del frontend..."
      (cd "$ROOT_DIR/frontend" && npm install)
    fi
    start_background "frontend-vite" npm --prefix "$ROOT_DIR/frontend" run dev -- \
      --host 0.0.0.0 --port 5173 --strictPort
    wait_for_port "frontend-vite" "127.0.0.1" 5173 30
  else
    echo "ERROR: npm no esta instalado. El nodo 1 necesita Node.js y npm para iniciar el frontend."
    exit 1
  fi
fi

NODE_ENV=(
  "NODE_ID=$NODE_ID"
  "NODE_HOST=$NODE_HOST"
  "TCP_PORT=$NODE_TCP_PORT"
  "SERVER_PORT=$NODE_HTTP_PORT"
  "CONSUL_HOST=$CONSUL_HOST"
  "AJUSTAR_RELOJ_SISTEMA=$AJUSTAR_RELOJ_SISTEMA"
  "TIMEOUT_HEARTBEAT_MS=${TIMEOUT_HEARTBEAT_MS:-15000}"
  "NODES_CONFIG_FILE=$ROOT_DIR/nodes.json"
)

if [[ "$AJUSTAR_RELOJ_SISTEMA" == "true" && "$(id -u)" != "0" ]]; then
  start_background "node$NODE_ID" sudo env "${NODE_ENV[@]}" \
    "$JAVA_CMD" -jar "$ROOT_DIR/backend/hospital-service/target/hospital-service.jar"
else
  start_background "node$NODE_ID" env "${NODE_ENV[@]}" \
    "$JAVA_CMD" -jar "$ROOT_DIR/backend/hospital-service/target/hospital-service.jar"
fi

wait_for_port "node$NODE_ID" "127.0.0.1" "$NODE_HTTP_PORT" 60
wait_for_port "node$NODE_ID" "127.0.0.1" "$NODE_TCP_PORT" 30

echo ""
linea
echo "Nodo $NODE_ID iniciado correctamente"
linea
echo "API del nodo:      http://$NODE_HOST:$NODE_HTTP_PORT"
echo "TCP del nodo:      $NODE_TCP_PORT"
echo "Consul principal:  http://$CONSUL_HOST:8500/ui"
if [[ "$es_principal" == true ]]; then
  echo ""
  echo "Abrir en esta PC:"
  echo "  Panel web:        http://localhost:5173"
  echo "  API Gateway:      http://localhost:8080"
  echo "  Consul UI:        http://localhost:8500/ui"
  echo ""
  echo "Abrir desde otras PCs de la misma red:"
  echo "  Panel web:        http://$NODE_HOST:5173"
  echo "  API Gateway:      http://$NODE_HOST:8080"
  echo "  Consul UI:        http://$NODE_HOST:8500/ui"
  echo ""
  echo "Si el panel no abre desde otra PC, revisa que esa PC use la IP de la PC 1 y no localhost."
else
  echo ""
  echo "Este nodo ya quedo conectado a la red."
  echo "Para ver el dashboard, abre en el navegador:"
  echo "  http://$CONSUL_HOST:5173"
fi
echo ""
echo "Logs utiles:"
echo "  tail -f $LOG_DIR/node$NODE_ID.out.log"
if [[ "$es_principal" == true ]]; then
  echo "  tail -f $LOG_DIR/frontend-vite.out.log"
  echo "  tail -f $LOG_DIR/gateway.out.log"
fi
linea
