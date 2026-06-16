#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="$ROOT_DIR/logs"

if [[ ! -d "$LOG_DIR" ]]; then
  echo "No existe directorio logs/. No hay procesos registrados por detener."
  exit 0
fi

for pid_file in "$LOG_DIR"/*.pid; do
  [[ -e "$pid_file" ]] || continue
  pid="$(cat "$pid_file")"
  name="$(basename "$pid_file" .pid)"
  if ps -p "$pid" >/dev/null 2>&1; then
    echo "Deteniendo $name PID=$pid"
    if ! kill "$pid" >/dev/null 2>&1; then
      sudo kill "$pid" >/dev/null 2>&1 || true
    fi
  else
    echo "$name ya no esta activo"
  fi
  rm -f "$pid_file"
done

echo "Buscando y limpiando procesos huerfanos..."

# Detener por nombre de proceso
pkill -f "api-gateway.jar" || sudo pkill -f "api-gateway.jar" || true
pkill -f "hospital-service.jar" || sudo pkill -f "hospital-service.jar" || true
pkill -f "consul agent" || sudo pkill -f "consul agent" || true

# Detener procesos escuchando en los puertos del proyecto
for port in 8500 8300 8080 5173 8081 8082 8083 8084 9001 9002 9003 9004; do
  if command -v lsof >/dev/null 2>&1; then
    pids=$(lsof -t -i :$port 2>/dev/null || true)
    if [[ -n "$pids" ]]; then
      echo "Liberando puerto $port (PIDs: $pids)..."
      kill -9 $pids 2>/dev/null || sudo kill -9 $pids 2>/dev/null || true
    fi
  fi
done

echo "Procesos detenidos."
