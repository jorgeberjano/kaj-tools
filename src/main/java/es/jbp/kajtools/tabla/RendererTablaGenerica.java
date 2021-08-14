package es.jbp.kajtools.tabla;

import java.awt.Color;
import java.awt.Component;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 *
 * @author jberjano
 */
public class RendererTablaGenerica extends DefaultTableCellRenderer {

    private ColoreadorFila coloreador;

    public void setColoreador(ColoreadorFila coloreador) {
        this.coloreador = coloreador;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        Component componente = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        setBorder(noFocusBorder);

        TablaGenerica tablaGenerica = table instanceof TablaGenerica
                ? (TablaGenerica) table : null;

//        if (!isSelected) {
//            componente.setBackground(table.getBackground());
//        }

        if (value instanceof Color) {
            componente.setBackground((Color) value);
            if (componente instanceof JLabel) {
                ((JLabel) componente).setText("");
            }
        } else if (!isSelected && coloreador != null && tablaGenerica != null) {

            ModeloTablaAbstracto modelo = tablaGenerica.getModelo();
            Object entidad = modelo.getFila(row);
            Color colorFondo = coloreador.determinarColorFondo(entidad);
            if (colorFondo != null) {
                componente.setBackground(colorFondo);
            }
            Color colorTexto = coloreador.determinarColorTexto(entidad);
            if (colorTexto != null) {
                componente.setForeground(colorTexto);
            }
        }

        return this;
    }

}
