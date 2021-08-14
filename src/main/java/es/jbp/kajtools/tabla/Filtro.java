package es.jbp.kajtools.tabla;

import java.util.List;

/**
 * Contrato que cumplen los filtros que se aplican a las tablas genericas
 * @author jberjano
 */
public interface Filtro<T> {
    List<T> filtrar(List<T> lista);
}
