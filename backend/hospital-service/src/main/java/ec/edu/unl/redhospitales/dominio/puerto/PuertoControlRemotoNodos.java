/*
 * Define acciones remotas para simular caida y recuperacion de un nodo concreto.
 */
package ec.edu.unl.redhospitales.dominio.puerto;

import ec.edu.unl.redhospitales.dominio.modelo.NodoHospitalario;

public interface PuertoControlRemotoNodos {

    boolean solicitarCaida(NodoHospitalario nodo);

    boolean solicitarRecuperacion(NodoHospitalario nodo);

    boolean solicitarSincronizacionCristian(NodoHospitalario nodo);

    boolean solicitarExclusion(NodoHospitalario nodo);

    boolean liberarExclusion(NodoHospitalario nodo);
}
