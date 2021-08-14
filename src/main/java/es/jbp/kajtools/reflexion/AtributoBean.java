package es.jbp.kajtools.reflexion;

/**
 *
 * @author jberjano
 */
public class AtributoBean {
    private String nombre;
    private Object valor;
    private Class clase;
    private int orden;    

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Object getValor() {
        return valor;
    }

    public void setValor(Object valor) {
        this.valor = valor;
    }

    public Class getClase() {
        return clase;
    }

    public void setClase(Class clase) {
        this.clase = clase;
    }

    public int getOrden() {
        return orden;
    }

    public void setOrden(int orden) {
        this.orden = orden;
    }    
}
