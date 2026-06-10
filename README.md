# Red de Hospitales Distribuida

Proyecto academico para simular una red distribuida de 4 hospitales con Java 21, Spring Boot 3, Spring Cloud Gateway, Consul, sockets TCP y React + Vite.

El sistema ejecuta varios procesos del mismo servicio hospitalario. Cada proceso representa un nodo, intercambia mensajes TCP con los demas, detecta fallos del coordinador mediante heartbeats, elige un nuevo coordinador con Bully, sincroniza tiempo con Cristian y expone un API REST consumido por el frontend a traves del API Gateway.

## Tecnologias usadas

- Java 21
- Spring Boot 3
- Spring Cloud Gateway
- Spring Cloud Consul Discovery
- TCP Sockets en Java
- Maven
- SLF4J
- React + Vite
- Framer Motion
- Lucide React
- PostgreSQL opcional, no requerido por esta version

No se usa Docker ni Docker Compose.

## Arquitectura general

```text
Frontend React
  -> API Gateway Spring Cloud Gateway, puerto 8080
  -> hospital-service, puertos HTTP 8081 a 8084
  -> Consul, puerto 8500, registro y descubrimiento
  -> Sockets TCP, puertos 9001 a 9004
```

El backend principal usa arquitectura hexagonal:

- `dominio`: modelos, enumeraciones y puertos sin dependencias de Spring.
- `aplicacion`: casos de uso y coordinacion de reglas.
- `infraestructura`: adaptadores TCP, Consul, memoria y control remoto.
- `presentacion`: controladores REST y DTOs.

## Instalar y ejecutar Consul manualmente

### Ubuntu

```bash
wget -O consul.zip https://releases.hashicorp.com/consul/1.19.2/consul_1.19.2_linux_amd64.zip
unzip consul.zip
sudo mv consul /usr/local/bin/
consul --version
consul agent -dev -client=0.0.0.0
```

### Windows

1. Descarga Consul desde `https://developer.hashicorp.com/consul/install`.
2. Agrega `consul.exe` al `PATH`.
3. Ejecuta:

```powershell
consul agent -dev -client=0.0.0.0
```

La consola web queda disponible en `http://localhost:8500`.

Consul no reemplaza a Bully. Consul registra y descubre servicios activos; Bully decide que nodo hospitalario es coordinador.

## Compilar backend

Desde la raiz:

```bash
mvn clean package
```

## Ejecutar los 4 nodos en una maquina

Abre una terminal por nodo desde la raiz del proyecto:

```bash
java -jar backend/hospital-service/target/hospital-service.jar --node.id=1 --server.port=8081 --tcp.port=9001
java -jar backend/hospital-service/target/hospital-service.jar --node.id=2 --server.port=8082 --tcp.port=9002
java -jar backend/hospital-service/target/hospital-service.jar --node.id=3 --server.port=8083 --tcp.port=9003
java -jar backend/hospital-service/target/hospital-service.jar --node.id=4 --server.port=8084 --tcp.port=9004
```

El nodo activo con ID mas alto debe quedar como coordinador. Con los 4 nodos activos, el coordinador esperado es el nodo 4.

## Ejecutar el API Gateway

En otra terminal:

```bash
java -jar backend/api-gateway/target/api-gateway.jar --server.port=8080
```

Rutas minimas expuestas por el gateway:

- `GET http://localhost:8080/api/hospitals`
- `GET http://localhost:8080/api/nodes`
- `GET http://localhost:8080/api/nodes/coordinator`
- `GET http://localhost:8080/api/logs`
- `POST http://localhost:8080/api/election/start`
- `POST http://localhost:8080/api/simulation/fail/{nodeId}`
- `POST http://localhost:8080/api/simulation/recover/{nodeId}`
- `POST http://localhost:8080/api/donors/compatibility`

## Ejecutar frontend

```bash
cd frontend
npm install
npm run dev
```

Abre `http://localhost:5173`. El frontend consume el gateway en `http://localhost:8080`. Para cambiarlo, crea `frontend/.env`:

```env
VITE_API_BASE_URL=http://localhost:8080
```

## Probar caida del coordinador

Con los 4 nodos activos, el coordinador inicial es el nodo 4. Puedes simular su caida desde el gateway:

```bash
curl -X POST http://localhost:8080/api/simulation/fail/4
```

Tambien puedes llamar directamente al nodo:

```bash
curl -X POST http://localhost:8084/api/simulation/fail/4
```

El nodo 4 deja de enviar heartbeats y deja de responder mensajes distribuidos. Tras mas de 5 segundos sin heartbeat, los seguidores inician eleccion Bully.

## Verificar eleccion Bully

Consulta el coordinador:

```bash
curl http://localhost:8080/api/nodes/coordinator
```

Despues de fallar el nodo 4, el coordinador esperado es el nodo 3. Los logs deben mostrar mensajes `ELECTION`, `ANSWER` y `COORDINATOR`.

Tambien puedes iniciar una eleccion manual:

```bash
curl -X POST http://localhost:8080/api/election/start
```

## Verificar heartbeats

Consulta logs:

```bash
curl http://localhost:8080/api/logs
```

El coordinador registra envio de `HEARTBEAT` cada 2 segundos. Los seguidores registran la recepcion y actualizan `ultimaSenal`.

## Verificar sincronizacion Cristian

Los seguidores solicitan tiempo al coordinador periodicamente mediante `SYNC_TIME_REQUEST`. El coordinador responde `SYNC_TIME_RESPONSE`. En los logs se registran hora local, hora recibida, latencia estimada y hora ajustada.

## Verificar Consul

Abre:

```text
http://localhost:8500/ui
```

Debe aparecer el servicio `hospital-service` con varias instancias y el servicio `api-gateway`.

Desde consola:

```bash
curl http://localhost:8500/v1/catalog/service/hospital-service
```

## Usar 4 laptops en el mismo router

Edita `nodes.json` en todas las laptops y cambia `localhost` por las IP reales:

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

Cada laptop ejecuta el mismo JAR, con parametros diferentes:

```bash
java -jar backend/hospital-service/target/hospital-service.jar --node.id=1 --server.port=8081 --tcp.port=9001 --node.host=192.168.1.10 --spring.cloud.consul.host=192.168.1.10
```

Cambia `node.id`, `server.port`, `tcp.port`, `node.host` y `spring.cloud.consul.host` segun corresponda. Asegurate de abrir los puertos HTTP, TCP y el puerto 8500 de Consul en el firewall.
