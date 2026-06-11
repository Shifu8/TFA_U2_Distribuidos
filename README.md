# Red de Hospitales Distribuida

Proyecto academico de Sistemas Distribuidos para ejecutar una red de 4 hospitales en computadoras reales conectadas al mismo router. Cada computadora representa un nodo independiente del sistema.

No se usa Docker, Docker Swarm ni Gunicorn. El backend esta hecho con Java 21 y Spring Boot, por eso cada nodo se ejecuta como un proceso Java independiente usando `java -jar`.

## Objetivo

El mismo backend debe poder copiarse a 4 PCs y ejecutarse cambiando solo:

- El `nodeId` usado al iniciar.
- Las IPs configuradas en `nodes.json`.

La comunicacion distribuida entre nodos se hace por sockets TCP sobre la red local. Consul se usa solo para descubrimiento de servicios.

## Tecnologias

- Java 21
- Spring Boot 3
- Spring Cloud Gateway
- Spring Cloud Consul Discovery
- Sockets TCP
- Maven
- React + Vite
- Bash scripts

## Arquitectura Por Capas

El backend esta separado por responsabilidades:

- `dominio`: modelos, enumeraciones y contratos.
- `aplicacion/servicio`: logica de coordinacion y algoritmos distribuidos.
- `infraestructura/tcp`: cliente y servidor TCP.
- `infraestructura/consul`: descubrimiento de servicios.
- `infraestructura/configuracion`: carga de `nodes.json`.
- `infraestructura/controlremoto`: llamadas HTTP internas entre nodos.
- `presentacion/controlador`: endpoints REST.
- `presentacion/dto`: respuestas para frontend.

Los controladores no implementan los algoritmos; solo llaman al servicio de aplicacion.

## Algoritmos Implementados

### Bully / Grandulon

El nodo activo con ID mas alto debe ser coordinador. El coordinador envia heartbeats. Si un seguidor deja de recibir heartbeat, inicia eleccion:

1. Envia `ELECTION` a nodos con ID mayor.
2. Si recibe `ANSWER`, espera `COORDINATOR`.
3. Si no recibe respuesta, se declara coordinador.
4. Notifica a los demas con `COORDINATOR`.

### Cristian

Los seguidores sincronizan su reloj con el coordinador:

1. El seguidor envia `SYNC_TIME_REQUEST`.
2. El coordinador responde con su hora.
3. El seguidor calcula RTT.
4. Estima latencia como `RTT / 2`.
5. Muestra hora local, hora del coordinador y hora ajustada.

### Exclusion Mutua Por Servidor Central

El coordinador actua como servidor central de concurrencia. Se usa una cola FIFO:

1. Un nodo envia `MUTEX_REQUEST`.
2. El coordinador concede acceso con `MUTEX_GRANT` si el recurso esta libre.
3. Si esta ocupado, el nodo queda en cola FIFO.
4. El nodo libera con `MUTEX_RELEASE`.
5. El coordinador concede acceso al siguiente nodo de la cola.

El recurso critico simulado es `directorio-donantes`.

## Patron Singleton

Se aplica Singleton en:

- `ConfiguracionNodoLocal`: mantiene la configuracion unica del nodo local: ID, host y datos base de ejecucion.
- `GestorConcurrencia`: mantiene una unica cola FIFO y un unico nodo propietario de la seccion critica dentro del coordinador.

Esto evita configuraciones duplicadas y multiples colas dentro del mismo nodo.

## Consul

Consul registra servicios activos y permite descubrir instancias. No decide coordinador ni ejecuta algoritmos.

En este sistema:

- Consul = descubrimiento de servicios.
- Bully = eleccion de coordinador.
- Cristian = sincronizacion de relojes.
- Servidor Central = exclusion mutua.

## Configuracion De Nodos

Editar `nodes.json` antes de la defensa con las IP reales entregadas por el router.

Formato actual soportado:

```json
{
  "nodos": [
    { "id": 1, "nombreHospital": "Hospital Isidro Ayora", "host": "192.168.1.10", "tcpPort": 9001, "httpPort": 8081 },
    { "id": 2, "nombreHospital": "Hospital UTPL", "host": "192.168.1.11", "tcpPort": 9002, "httpPort": 8082 },
    { "id": 3, "nombreHospital": "Hospital Clinica San Agustin", "host": "192.168.1.12", "tcpPort": 9003, "httpPort": 8083 },
    { "id": 4, "nombreHospital": "Hospital Manuel Ygnacio Monteros", "host": "192.168.1.13", "tcpPort": 9004, "httpPort": 8084 }
  ]
}
```

Tambien se soporta el formato simple:

```json
[
  { "id": 1, "host": "192.168.1.10", "port": 9001, "apiPort": 8081 }
]
```

## Ejecucion En 4 PCs Con Router

Todas las PCs deben tener el proyecto copiado y Java 21 instalado. La PC 1 es la principal porque levanta Consul, API Gateway y frontend.

### PC 1

```bash
./scripts/iniciar-nodo.sh 1
```

Levanta:

- Consul
- API Gateway
- Frontend
- Nodo 1

### PC 2

```bash
./scripts/iniciar-nodo.sh 2
```

### PC 3

```bash
./scripts/iniciar-nodo.sh 3
```

### PC 4

```bash
./scripts/iniciar-nodo.sh 4
```

Si el sistema no permite ejecutar directo el `.sh`, usar:

```bash
bash scripts/iniciar-nodo.sh 1
```

## URLs De La Defensa

Si la PC principal tiene IP `192.168.1.10`:

- Dashboard: `http://192.168.1.10:5173`
- API Gateway: `http://192.168.1.10:8080`
- Consul UI: `http://192.168.1.10:8500`

El profesor puede abrir el dashboard desde cualquier computadora conectada al mismo router.

En la defensa no es obligatorio que el profesor abra nada. Ustedes pueden proyectar la PC observadora o la PC principal y mostrar el dashboard desde ahi.

## PC Observadora

Si una computadora no sera nodo, puede usarse solo para ver la aplicacion. Debe estar conectada al mismo router y abrir:

```text
http://IP_DE_PC_1:5173
```

Esa PC no ejecuta `iniciar-nodo.sh`; solo abre el navegador.

## Cristian Cambiando La Hora Del Sistema En Ubuntu

Por defecto, Cristian calcula y muestra la hora ajustada sin modificar el reloj del sistema operativo. Para que realmente cambie la hora de Ubuntu, iniciar cada nodo con:

```bash
AJUSTAR_RELOJ_SISTEMA=true bash scripts/iniciar-nodo.sh 2
```

El script pedira permisos `sudo` al inicio porque cambiar la hora del sistema requiere privilegios. Internamente el nodo usa comandos de Ubuntu como `timedatectl` y `date`.

Ejemplo para las 4 PCs:

```bash
AJUSTAR_RELOJ_SISTEMA=true bash scripts/iniciar-nodo.sh 1
AJUSTAR_RELOJ_SISTEMA=true bash scripts/iniciar-nodo.sh 2
AJUSTAR_RELOJ_SISTEMA=true bash scripts/iniciar-nodo.sh 3
AJUSTAR_RELOJ_SISTEMA=true bash scripts/iniciar-nodo.sh 4
```

Para evitar que Ubuntu vuelva a corregir la hora automaticamente, el nodo intenta desactivar NTP con `timedatectl set-ntp false`.

## Prueba Local En Una Sola PC

Con `nodes.json` usando `localhost`, se puede levantar todo:

```bash
./scripts/iniciar-red.sh
```

Para detener:

```bash
./scripts/detener-red.sh
```

Los logs generados por scripts se guardan en `logs/`.

## Endpoints Minimos

Endpoints academicos:

- `GET /estado`
- `GET /nodos`
- `GET /coordinador`
- `POST /eleccion/iniciar`
- `POST /sincronizacion/cristian`
- `POST /exclusion/solicitar`
- `POST /exclusion/liberar`
- `POST /solicitar_organo`
- `GET /logs?algoritmo=bully`
- `GET /logs?algoritmo=cristian`
- `GET /logs?algoritmo=exclusion`
- `GET /logs?algoritmo=red`

Endpoints usados por el dashboard:

- `GET /api/estado`
- `GET /api/nodes`
- `GET /api/nodes/coordinator`
- `POST /api/election/start`
- `POST /api/synchronization/cristian/{nodeId}`
- `POST /api/exclusion/request/{nodeId}`
- `POST /api/exclusion/release/{nodeId}`
- `GET /api/logs`

## Pruebas Para La Defensa

1. Levantar los 4 nodos en 4 PCs.
2. Abrir Consul y mostrar los servicios registrados.
3. Abrir el dashboard desde la IP de la PC 1.
4. Verificar que el coordinador inicial sea el nodo activo con ID mas alto.
5. Fallar el coordinador desde el dashboard.
6. Verificar nueva eleccion Bully.
7. Ejecutar sincronizacion Cristian y mostrar RTT.
8. Solicitar seccion critica desde dos nodos distintos.
9. Mostrar la cola FIFO.
10. Liberar la seccion critica y ver pasar el permiso al siguiente nodo.
11. Filtrar logs por `BULLY`, `CRISTIAN`, `EXCLUSION` y `RED`.
