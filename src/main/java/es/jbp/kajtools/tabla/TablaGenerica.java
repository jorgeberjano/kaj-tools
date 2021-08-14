package es.jbp.kajtools.tabla;

import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

/**
 * Tabla generica que usa el modelo generico. Uso:
 *      List<Entidad> entidades = new ArrayList<Entidad>();
 *      ModeloTablaGenerico<Entidad> modeloTabla;
 *      TablaGenerica tabla;
 *      modeloTabla = new ModeloTablaGenerico<>(entidades);
 *      modeloTabla.agregarColumna("nombre", "Nombre");
 *      modeloTabla.agregarColumna("descripcion", "Descripción");
 *      tabla = new TablaGenerica(modeloTabla);
 * @author jberjano
 */
public class TablaGenerica extends JTable {

    protected int columnaOrden = 0;
    protected boolean ordenAscendente = true;
    private ModeloTablaAbstracto modelo;
    private RendererTablaGenerica renderer;
  
    public TablaGenerica() {
        JTableHeader header = getTableHeader();
        header.setUpdateTableInRealTime(true);
        header.setReorderingAllowed(false);        
        header.addMouseListener(new ColumnListener(this));

        this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);        
        this.setRowSelectionAllowed(true);
               
        renderer = new RendererTablaGenerica();
        setDefaultRenderer(Object.class, renderer);
    }
        
    public TablaGenerica(ModeloTablaAbstracto tableModel) {
        this();
        setModelo(tableModel);
    }    
    
    @Override
    public void setFont(Font font) {
        JTableHeader header = getTableHeader();
        header.setFont(font);        
        
        //FontMetrics fm = getGraphics().getFontMetrics(font);
        int height = font.getSize() * 2;
        this.setRowHeight(height);
        
        super.setFont(font);
    }
    
    
    @Override
    public void setModel(TableModel model) {
        if (model instanceof ModeloTablaAbstracto) {
            setModelo((ModeloTablaAbstracto) model);
        } else {
            super.setModel(model);
        }
    }
    
    public ModeloTablaAbstracto getModelo() {
        return modelo;
    }
    
    public final void setModelo(ModeloTablaAbstracto modelo) {
        this.modelo = modelo;
        super.setModel(modelo);

        TableColumnModel colModel = getColumnModel();
        for (int i = 0; i < colModel.getColumnCount(); i++) {
            TableColumn columna = colModel.getColumn(i);
            columna.setHeaderValue(modelo.getTitulo(columna.getModelIndex()));
            columna.setMinWidth(10);
            Integer ancho = modelo.getAncho(i);
            if (ancho != null) {                
                columna.setPreferredWidth(ancho);
            }
        }
    }

    public void setColoreador(ColoreadorFila coloreadorFila) {
        renderer.setColoreador(coloreadorFila);
    }

    public class ColumnListener extends MouseAdapter {

        protected JTable table;

        public ColumnListener(JTable t) {
            table = t;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            
            if (modelo == null) {
                return;
            }
            
            if (e.getButton() != MouseEvent.BUTTON1) {
                return;
            }
            TableColumnModel colModel = table.getColumnModel();
            int columnModelIndex = colModel.getColumnIndexAtX(e.getX());
            int modelIndex = colModel.getColumn(columnModelIndex).getModelIndex();

            if (modelIndex < 0) {
                return;
            }
            if (columnaOrden == modelIndex) {
                ordenAscendente = !ordenAscendente;
            } else {
                columnaOrden = modelIndex;
            }

            //String atributo = modelo.getAtributo(modelIndex);
            modelo.ordenarPor(modelIndex, ordenAscendente);
            table.repaint();
        }
    }

}
