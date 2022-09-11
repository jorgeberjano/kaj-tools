package es.jbp.tabla;

import java.awt.Color;

/**
 * Contrato que deben cumplir los objetos encargados de decidir el color
 * de fondo y de texto que tendrá de la fila en la que se representan los datos
 * de una entidad en una tabla genérica.
 * @author jberjano
 */
public interface ColoreadorFila<T> {
    Color determinarColorTexto(T entidad);
    Color determinarColorFondo(T entidad);
}
