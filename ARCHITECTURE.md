# Arquitectura

El proyecto se organiza como una aplicacion distribuida por capas. Cada proceso Java representa un nodo hospitalario independiente y se comunica con los demas mediante sockets TCP.

## Capas

- `dominio`: modelos, enumeraciones y contratos del sistema.
- `aplicacion`: servicios de negocio distribuido y coordinacion.
- `infraestructura`: adaptadores concretos para TCP, Consul, configuracion JSON, memoria y llamadas HTTP internas.
- `presentacion`: controladores REST y DTOs usados por el frontend.

Los controladores REST solo exponen operaciones. La logica de Bully, Cristian y exclusion mutua vive en la capa de aplicacion.

## Flujo General

```text
Frontend React
  -> API Gateway Spring Cloud Gateway, puerto 8080
  -> hospital-service, puertos HTTP 8081 a 8084
  -> sockets TCP entre nodos, puertos 9001 a 9004
  -> Consul, puerto 8500, solo descubrimiento
```

## Consul

Consul registra instancias activas y permite que el API Gateway descubra nodos. No participa en la coordinacion distribuida.

## Bully

Bully elige como coordinador al nodo activo con ID mas alto. Si el coordinador cae y deja de enviar heartbeats, los seguidores inician eleccion por mensajes TCP.

## Cristian

Los seguidores consultan la hora del coordinador por TCP, calculan RTT y muestran hora local, hora del coordinador y hora ajustada.

## Exclusion Mutua Por Servidor Central

El coordinador mantiene una unica cola FIFO mediante `GestorConcurrencia`. Solo un nodo puede entrar a la seccion critica `directorio-donantes`. Al liberar, el coordinador concede acceso al siguiente nodo de la cola.

## Comunicacion TCP

Los mensajes se serializan como JSON de una linea. Cada nodo tiene un `ServerSocket` y envia mensajes con un cliente TCP.

Tipos principales:

- `HEARTBEAT`
- `ELECTION`
- `ANSWER`
- `COORDINATOR`
- `SYNC_TIME_REQUEST`
- `SYNC_TIME_RESPONSE`
- `MUTEX_REQUEST`
- `MUTEX_GRANT`
- `MUTEX_RELEASE`
- `DONOR_REQUEST`
- `DONOR_RESPONSE`

## Despliegue LAN

En 4 PCs reales, cada PC ejecuta el mismo JAR con diferente ID. `nodes.json` contiene las IPs entregadas por el router.

La PC 1 levanta Consul, API Gateway, frontend y nodo 1. Las PCs 2, 3 y 4 levantan sus nodos respectivos.
