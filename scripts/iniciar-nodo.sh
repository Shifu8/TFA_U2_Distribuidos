#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="$ROOT_DIR/logs"
NODE_ID="${1:-}"
AJUSTAR_RELOJ_SISTEMA="${AJUSTAR_RELOJ_SISTEMA:-false}"

if [[ -z "$NODE_ID" ]]; then
  echo "Uso: ./scripts/iniciar-nodo.sh <nodeId>"
  echo "Ejemplo: ./scripts/iniciar-nodo.sh 1"
  exit 1
fi

mkdir -p "$LOG_DIR"

if [[ "$AJUSTAR_RELOJ_SISTEMA" == "true" ]]; then
  echo "Cristian cambiara la hora del sistema en este nodo. Validando permisos sudo..."
  sudo -v
fi

command_exists() {
  command -v "$1" >/dev/null 2>&1
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

start_background() {
  local name="$1"
  shift
  local pid_file="$LOG_DIR/$name.pid"

  if [[ -f "$pid_file" ]] && kill -0 "$(cat "$pid_file")" >/dev/null 2>&1; then
    echo "$name ya esta ejecutandose con PID $(cat "$pid_file")"
    return
  fi

  echo "Iniciando $name..."
  nohup "$@" > "$LOG_DIR/$name.out.log" 2> "$LOG_DIR/$name.err.log" &
  echo $! > "$pid_file"
}

cd "$ROOT_DIR"

if [[ ! -f "$ROOT_DIR/backend/hospital-service/target/hospital-service.jar" || ! -f "$ROOT_DIR/backend/api-gateway/target/api-gateway.jar" ]]; then
  echo "Compilando backend con Maven..."
  mvn clean package -DskipTests
fi

if [[ "$es_principal" == true ]]; then
  if ! command_exists consul; then
    echo "Consul no esta instalado o no esta en PATH. Instala Consul antes de iniciar la PC principal."
    exit 1
  fi

  start_background "consul" consul agent -dev -client=0.0.0.0
  sleep 3

  start_background "gateway" env CONSUL_HOST="$CONSUL_HOST" GATEWAY_HOST="$NODE_HOST" \
    java -jar "$ROOT_DIR/backend/api-gateway/target/api-gateway.jar" --server.port=8080

  if command_exists npm; then
    if [[ ! -d "$ROOT_DIR/frontend/node_modules" ]]; then
      echo "Instalando dependencias del frontend..."
      (cd "$ROOT_DIR/frontend" && npm install)
    fi
    start_background "frontend-vite" bash -lc "cd '$ROOT_DIR/frontend' && npm run dev -- --host 0.0.0.0"
  else
    echo "npm no esta instalado; se omite el frontend en esta PC."
  fi
fi

start_background "node$NODE_ID" env \
  NODE_ID="$NODE_ID" \
  NODE_HOST="$NODE_HOST" \
  TCP_PORT="$NODE_TCP_PORT" \
  SERVER_PORT="$NODE_HTTP_PORT" \
  CONSUL_HOST="$CONSUL_HOST" \
  AJUSTAR_RELOJ_SISTEMA="$AJUSTAR_RELOJ_SISTEMA" \
  NODES_CONFIG_FILE="$ROOT_DIR/nodes.json" \
  java -jar "$ROOT_DIR/backend/hospital-service/target/hospital-service.jar"

echo ""
echo "Nodo $NODE_ID iniciado"
echo "Host:    $NODE_HOST"
echo "API:     http://$NODE_HOST:$NODE_HTTP_PORT"
echo "TCP:     $NODE_TCP_PORT"
echo "Consul:  http://$CONSUL_HOST:8500"
if [[ "$es_principal" == true ]]; then
  echo "Gateway: http://$NODE_HOST:8080"
  echo "Panel:   http://$NODE_HOST:5173"
fi
