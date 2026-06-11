#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo "Iniciando red distribuida local completa..."
echo "Este modo asume que nodes.json usa localhost o la IP local de esta maquina."

bash "$ROOT_DIR/scripts/iniciar-nodo.sh" 1
sleep 2
bash "$ROOT_DIR/scripts/iniciar-nodo.sh" 2
sleep 1
bash "$ROOT_DIR/scripts/iniciar-nodo.sh" 3
sleep 1
bash "$ROOT_DIR/scripts/iniciar-nodo.sh" 4

echo ""
echo "Red local iniciada."
echo "Dashboard: http://localhost:5173"
echo "Gateway:   http://localhost:8080"
echo "Consul:    http://localhost:8500"
