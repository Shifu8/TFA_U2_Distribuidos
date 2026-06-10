# Arquitectura

## Arquitectura hexagonal

El proyecto usa arquitectura hexagonal en `hospital-service` para mantener la logica distribuida lejos de los detalles tecnicos.

- Dominio: contiene modelos y contratos. No importa Spring Boot.
- Aplicacion: ejecuta casos de uso como Bully, heartbeats, Cristian y compatibilidad de donantes.
- Infraestructura: implementa puertos con TCP, Consul, memoria, JSON y llamadas HTTP remotas.
- Presentacion: expone controladores REST para el frontend.

Esta separacion permite cambiar un adaptador, por ejemplo TCP o persistencia, sin modificar el nucleo de coordinacion.

## Flujo Frontend -> API Gateway -> Backend

El frontend llama al API Gateway en `http://localhost:8080`. El gateway mantiene rutas `/api/**` y las envia a `lb://hospital-service`. El balanceo usa instancias registradas en Consul.

El servicio hospitalario responde REST y tambien participa en la red distribuida por TCP.

## Consul

Consul registra procesos y permite descubrir instancias disponibles. Cada nodo hospitalario usa:

- Nombre de servicio: `hospital-service`
- Instance ID: `hospital-service-{node.id}`
- Health check: `/actuator/health`

El API Gateway tambien se registra como `api-gateway`.

Consul ayuda a saber que procesos existen, pero no elige coordinador. La eleccion se mantiene como responsabilidad del algoritmo Bully.

## Bully

Bully usa el ID numerico de cada nodo. El nodo activo con ID mas alto gana.

Cuando un seguidor no recibe heartbeat del coordinador durante el tiempo configurado, envia `ELECTION` a nodos con ID mayor. Si recibe `ANSWER`, espera a que un nodo superior publique `COORDINATOR`. Si no recibe respuesta, se declara coordinador y notifica a todos.

## Cristian

Cristian sincroniza tiempo de forma aproximada. Un seguidor pregunta la hora al coordinador, mide el viaje de ida y vuelta, estima latencia como `RTT / 2` y ajusta la hora recibida.

El sistema registra:

- Hora local antes de ajustar.
- Hora recibida del coordinador.
- Latencia estimada.
- Hora ajustada.

## Comunicacion TCP

Cada nodo abre un `ServerSocket` en su puerto TCP. Los mensajes se serializan como JSON de una sola linea y se leen con `BufferedReader`.

Tipos principales:

- `HEARTBEAT`: coordinador a seguidores.
- `ELECTION`: solicitud de eleccion.
- `ANSWER`: respuesta de un nodo con ID mayor.
- `COORDINATOR`: anuncio del nuevo coordinador.
- `SYNC_TIME_REQUEST` y `SYNC_TIME_RESPONSE`: algoritmo de Cristian.
- `DONOR_REQUEST` y `DONOR_RESPONSE`: simulacion de consulta clinica.

## Despliegue localhost

Usa el `nodes.json` incluido, con host `localhost`, y ejecuta 4 procesos en puertos HTTP 8081 a 8084 y TCP 9001 a 9004.

## Despliegue en 4 laptops

Todas las laptops deben tener el mismo codigo y el mismo `nodes.json`, pero con IPs reales de la red local:

- Nodo 1: `192.168.1.10:9001`
- Nodo 2: `192.168.1.11:9002`
- Nodo 3: `192.168.1.12:9003`
- Nodo 4: `192.168.1.13:9004`

Cada laptop ejecuta el mismo JAR con diferente `node.id`, `server.port`, `tcp.port` y `node.host`. Consul puede ejecutarse en una laptop accesible para todas, usando `spring.cloud.consul.host` con esa IP.
