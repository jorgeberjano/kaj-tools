package es.jbp.kajtools.tabla;

import es.jbp.kajtools.reflexion.Reflexion;
import java.util.Comparator;

/**
 * Comparador de atributos de un objeto para ordenación de listas de entidades.
 *
 * @author jberjano
 */
public class ComparadorAtributo implements Comparator<Object> {

    private String nombreAtributo;
    private boolean invertirResultado = false;
    
    public ComparadorAtributo() {
        
    }
            
    public ComparadorAtributo(String nombreAtributo) {
        this.nombreAtributo = nombreAtributo;
    }
    
    public ComparadorAtributo(String nombreAtributo, boolean invertirResultado) {
        this.nombreAtributo = nombreAtributo;
        this.invertirResultado = invertirResultado;
    }

    @Override
    public int compare(Object objeto1, Object objeto2) {
        
        Object valor1 = obtenerValorAtributo(objeto1);
        Object valor2 = obtenerValorAtributo(objeto2);
        
        if (valor1 == null && valor2 == null) {
            return 0;
        } else if (valor1 == null) {
            return invertirResultado ? -1 : 1;
        } else if (valor2 == null) {
            return invertirResultado ? 1 : -1;
        }
        
        if (valor1.getClass() != valor2.getClass()) {
            valor1 = valor1.toString();            
            valor2 = valor2.toString();
        }
  
        int diferencia = 0;
        if (valor1 instanceof Integer) {
            diferencia = (Integer) valor1 - (Integer) valor2;
        } else if (valor1 instanceof Long) {
            diferencia = (int) ((Long) valor1 - (Long) valor2);
        } else if (valor1 instanceof Float) {
            diferencia = (int) ((Float) valor1 - (Float) valor2);
        } else if (valor1 instanceof Double) {
            diferencia = (int) ((Double) valor1 - (Double) valor2);
        } else {
            diferencia = valor1.toString().compareTo(valor2.toString());
        }
        
        if (invertirResultado) {
            return diferencia;
        } else {
            return -diferencia;            
        }
    }  

    private Object obtenerValorAtributo(Object objeto) {
        return Reflexion.obtenerValorAtributo(objeto, nombreAtributo);
    }

}
