package es.jbp.kajtools.tabla;

import javax.swing.table.AbstractTableModel;

/**
 *
 * @author jberjano
 */
public abstract class ModeloTablaAbstracto extends AbstractTableModel {

    public abstract Object getTitulo(int indice);

    public abstract Integer getAncho(int indice);

    public abstract void ordenarPor(int indice, boolean ordenAscendente);

    public abstract Object getFila(int fila);
}
