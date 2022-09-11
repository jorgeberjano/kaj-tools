package es.jbp.tabla;

import es.jbp.kajtools.reflexion.Reflexion;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Modelo genérico para tablas.
 *
 * @author jberjano
 */
public class ModeloTablaGenerico<T> extends ModeloTablaAbstracto {

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss.SSS");

    private transient List<T> listaCompleta;
    private transient List<T> listaActual;
    private final List<Columna> listaColumnas = new ArrayList<>();

    private static class Columna {
        String atributo;
        String titulo;
        Integer ancho;
    }

    public ModeloTablaGenerico() {
    }

    public ModeloTablaGenerico(List<T> lista) {
        this.listaActual = lista;
        this.listaCompleta = lista;
    }

    public void agregarColumna(String atributo, String titulo) {
        agregarColumna(atributo, titulo, null);
    }

    public void agregarColumna(String atributo, String titulo, Integer ancho) {
        Columna columna = new Columna();
        columna.atributo = atributo;
        columna.titulo = titulo;
        columna.ancho = ancho;
        listaColumnas.add(columna);
    }

    public void setListaObjetos(List<T> listaObjetos) {
        this.listaCompleta = listaObjetos;
        this.listaActual = listaObjetos;
        actualizar();
    }

    public List<T> getListaObjetos() {
        return this.listaCompleta;
    }

    public T getFila(int indice) {
        if (listaActual == null || indice < 0 || indice >= listaActual.size()) {
            return null;
        }
        return listaActual.get(indice);
    }

    @Override
    public String getColumnName(int nColumna) {
        if (nColumna < 0 || nColumna >= listaColumnas.size()) {
            return "";
        }
        return listaColumnas.get(nColumna).titulo;
    }

    @Override
    public int getRowCount() {
        return listaActual != null ? listaActual.size() : 0;
    }

    @Override
    public int getColumnCount() {
        return listaColumnas.size();
    }

    private boolean esColumnaValida(int nColumna) {
        return nColumna >= 0 && nColumna < listaColumnas.size();
    }

    public String getAtributo(int nColumna) {
        return esColumnaValida(nColumna) ? listaColumnas.get(nColumna).atributo : null;
    }

    public String getTitulo(int nColumna) {
        return esColumnaValida(nColumna) ? listaColumnas.get(nColumna).titulo : "";
    }

    public Integer getAncho(int nColumna) {
        return esColumnaValida(nColumna) ? listaColumnas.get(nColumna).ancho : null;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {

        String atributo = getAtributo(columnIndex);

        T fila = getFila(rowIndex);
        if (fila == null) {
            return null;
        }

        return obtenerValorAtributo(fila, atributo);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    private Object obtenerValorAtributo(Object objeto, String atributo) {

        Object valor = Reflexion.obtenerValorAtributo(objeto, atributo);

        if (valor instanceof Boolean) {
            valor = (Boolean) valor ? "Sí" : "No";
        } else if (valor instanceof LocalDateTime) {

            valor = ((LocalDateTime) valor).format(dateTimeFormatter);
        }

        return valor;
    }

    public Object getObjectAt(int rowIndex) {
        Object objeto = null;
        if (listaActual != null && rowIndex >= 0 && listaActual.size() > rowIndex) {
            objeto = listaActual.get(rowIndex);
        }
        return objeto;
    }

    public void ordenarPor(int indice, boolean ordenAscendente) {
        if (listaActual == null) {
            return;
        }
        String atributo = getAtributo(indice);
        ComparadorAtributo comparador = new ComparadorAtributo(atributo, ordenAscendente);
        listaActual.sort(comparador);
        actualizar();
    }

    public void filtrar(Filtro<T> filtro) {

        if (filtro != null) {
            listaActual = filtro.filtrar(listaCompleta);
        } else {
            listaActual = listaCompleta;
        }
        actualizar();
    }

    public void filtrarPorPredicado(Predicate<T> filtro) {
        listaActual = listaCompleta.stream().filter(filtro).collect(Collectors.toList());
        actualizar();
    }


    public void actualizar() {
        fireTableDataChanged();
    }
}
