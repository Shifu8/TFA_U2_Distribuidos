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

echo "Procesos detenidos."
