# Requerimientos del Sistema

## Requerimientos funcionales

1. El sistema debe representar 4 nodos hospitalarios distribuidos.
2. Cada nodo debe ejecutarse como un proceso Spring Boot independiente.
3. Los nodos deben comunicarse mediante sockets TCP.
4. El sistema debe registrar los nodos en Consul.
5. El sistema debe consultar nodos activos mediante Consul.
6. El coordinador debe enviar heartbeats cada 2 segundos.
7. Los seguidores deben detectar ausencia de heartbeat despues de 5 segundos.
8. Si el coordinador falla, los seguidores deben ejecutar el algoritmo Bully.
9. El nodo activo con mayor ID debe convertirse en coordinador.
10. El nuevo coordinador debe notificar a los demas con un mensaje `COORDINATOR`.
11. Los seguidores deben sincronizar su tiempo con el coordinador usando Cristian.
12. El sistema debe registrar eventos relevantes en logs.
13. El API Gateway debe redirigir llamadas del frontend al servicio hospitalario.
14. El frontend debe mostrar nodos, coordinador, estado, logs y acciones de simulacion.
15. El sistema debe simular consultas de compatibilidad de donantes.

## Requerimientos no funcionales

1. El backend debe usar Java 21 y Spring Boot 3.
2. El frontend debe usar React + Vite.
3. El codigo del servicio hospitalario debe organizarse con arquitectura hexagonal.
4. La capa de dominio no debe depender de Spring Boot.
5. Los adaptadores TCP, Consul y persistencia en memoria deben vivir en infraestructura.
6. Los controladores REST deben vivir en presentacion.
7. Los nombres de clases, paquetes, DTOs y servicios deben estar en espanol cuando sea posible.
8. El sistema no debe usar Docker ni Docker Compose.
9. Debe poder ejecutarse en una maquina o en 4 laptops conectadas al mismo router.
10. Debe usar SLF4J para logs.

## Arquitectura

```text
React + Vite
  -> Spring Cloud Gateway
  -> hospital-service
  -> Consul
  -> TCP Sockets entre nodos
```

El servicio hospitalario separa:

- `dominio`: modelos `NodoHospitalario`, `MensajeTcp`, `EventoSistema` y enumeraciones.
- `dominio.puerto`: contratos para comunicacion, configuracion, logs, Consul y control remoto.
- `aplicacion.servicio`: coordinacion Bully, heartbeat, Cristian, consultas y simulacion.
- `infraestructura`: adaptadores concretos TCP, Consul, memoria, JSON y HTTP remoto.
- `presentacion`: controladores REST y DTOs.

## Uso de Consul

Consul funciona como registro y descubrimiento. Cada proceso `hospital-service` se registra con nombre `hospital-service` e `instance-id` basado en su `node.id`. El API Gateway usa `lb://hospital-service` para enviar solicitudes hacia instancias registradas.

Consul no decide el coordinador. La eleccion de coordinador se hace con Bully dentro de la red TCP.

## Algoritmo Bully

1. Cada nodo conoce los IDs de los demas nodos mediante `nodes.json`.
2. Si un seguidor deja de recibir heartbeat del coordinador, inicia eleccion.
3. El nodo envia `ELECTION` a nodos con ID mayor.
4. Los nodos mayores responden `ANSWER`.
5. Si no recibe respuestas, el nodo se declara `COORDINADOR`.
6. El coordinador envia `COORDINATOR` a todos los demas.
7. Cada evento se registra en logs.

## Algoritmo de Cristian

1. Un seguidor guarda el instante local de envio.
2. Envia `SYNC_TIME_REQUEST` al coordinador.
3. El coordinador responde con su hora actual en `SYNC_TIME_RESPONSE`.
4. El seguidor calcula latencia aproximada como mitad del viaje de ida y vuelta.
5. La hora ajustada es hora del coordinador mas latencia estimada.
6. El resultado se registra en logs.

## Comunicacion TCP

Los nodos intercambian `MensajeTcp` serializados como JSON en una linea. Tipos soportados:

- `HEARTBEAT`
- `ELECTION`
- `ANSWER`
- `COORDINATOR`
- `SYNC_TIME_REQUEST`
- `SYNC_TIME_RESPONSE`
- `DONOR_REQUEST`
- `DONOR_RESPONSE`

Cada mensaje incluye origen, destino, contenido y marca de tiempo. La exclusion mutua se implementa con Servidor Central y cola FIFO.

## Comandos para ejecutar

Compilar:

```bash
mvn clean package
```

Ejecutar nodos:

```bash
java -jar backend/hospital-service/target/hospital-service.jar --node.id=1 --server.port=8081 --tcp.port=9001
java -jar backend/hospital-service/target/hospital-service.jar --node.id=2 --server.port=8082 --tcp.port=9002
java -jar backend/hospital-service/target/hospital-service.jar --node.id=3 --server.port=8083 --tcp.port=9003
java -jar backend/hospital-service/target/hospital-service.jar --node.id=4 --server.port=8084 --tcp.port=9004
```

Ejecutar gateway:

```bash
java -jar backend/api-gateway/target/api-gateway.jar --server.port=8080
```

Ejecutar frontend:

```bash
cd frontend
npm install
npm run dev
```

## Casos de prueba

1. Levantar Consul y verificar `http://localhost:8500/ui`.
2. Levantar los 4 nodos y verificar que todos registran health check.
3. Consultar `GET /api/nodes` y verificar los 4 hospitales.
4. Consultar `GET /api/nodes/coordinator` y verificar que el nodo 4 es coordinador.
5. Consultar `GET /api/logs` y observar heartbeats.
6. Ejecutar `POST /api/simulation/fail/4`.
7. Esperar mas de 5 segundos y verificar eleccion Bully.
8. Confirmar que el nodo 3 queda como coordinador.
9. Ejecutar `POST /api/election/start` y revisar logs.
10. Revisar logs de `SYNC_TIME_REQUEST` y `SYNC_TIME_RESPONSE`.
11. Enviar una consulta de donante con `POST /api/donors/compatibility`.
12. Recuperar el nodo 4 con `POST /api/simulation/recover/4` y verificar nueva eleccion.
