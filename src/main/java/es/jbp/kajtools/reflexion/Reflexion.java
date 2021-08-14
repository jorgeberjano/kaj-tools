package es.jbp.kajtools.reflexion;

import java.awt.Color;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

/**
 * Clase con utilidades para usar reflexión sobre objetos.
 *
 * @author jberjano
 */
public class Reflexion {

    public static Object obtenerValorAtributo(Object objeto, String nombreAtributo) {
        String[] listaNombres = nombreAtributo.split("\\.");
        if (listaNombres.length <= 1) {
            return obtenerValorAtributoSimple(objeto, nombreAtributo);
        }

        Object valor = null;
        for (String nombre : listaNombres) {
            valor = obtenerValorAtributoSimple(objeto, nombre);
            objeto = valor;
        }
        return valor;
    }

    public static Object obtenerValorAtributoSimple(Object objeto, String nombreAtributo) {
        if (objeto == null) {
            return null;
        }

        Object valor = null;
        try {
            Method metodo = new PropertyDescriptor(nombreAtributo, objeto.getClass()).getReadMethod();
            metodo.setAccessible(true);
            if (metodo != null) {
                valor = metodo.invoke(objeto);
            }
        } catch (Exception ex) {
            // TODO: cambiar la forma de reportar los errores
            ex.printStackTrace();
        }

        return valor;
    }

    /**
     * Devuelve una lista con cada uno de los valores de un atributo de cada
     * elemento de la lista de objetos.
     */
    public static ArrayList obtenerListaValoresAtributo(List listaObjetos, String nombreAtributo) {
        ArrayList lista = new ArrayList();
        Set conjunto = new HashSet();

        if (listaObjetos == null) {
            return lista;
        }

        for (Object elemento : listaObjetos) {
            Object valor = obtenerValorAtributo(elemento, nombreAtributo);
            if (valor != null && !conjunto.contains(valor)) {
                conjunto.add(valor);
                lista.add(valor);
            }
        }

        Collections.sort(lista);
        return lista;
    }

    /**
     * Devuelve una lista con cada uno de los valores distintos de un atributo
     * de cada elemento de la lista de objetos.
     */
    public static List obtenerListaValoresDistintosAtributo(List listaObjetos, String nombreAtributo) {
        List lista = new ArrayList();
        HashSet conjunto = new HashSet();

        if (listaObjetos == null) {
            return lista;
        }

        for (Object elemento : listaObjetos) {
            Object valor = obtenerValorAtributo(elemento, nombreAtributo);
            if (!conjunto.contains(valor)) {
                conjunto.add(valor);
                lista.add(valor);
            }
        }
        return lista;
    }

    public static boolean asignarValorAtributoSimple(Object objeto, String nombreAtributo, Object valor) {
        if (objeto == null) {
            return false;
        }

        try {
            Method metodo = new PropertyDescriptor(nombreAtributo, objeto.getClass()).getWriteMethod();
            if (metodo != null) {
                Class[] tipos = metodo.getParameterTypes();
                if (tipos.length != 1) {
                    return false;
                }
                Class tipo = tipos[0];
                Object valorTipado = convertir(tipo, valor);

                metodo.invoke(objeto, valorTipado);
            }
        } catch (Exception ex) {
            return false;
            //ex.printStackTrace();
        }
        return true;
    }

    private static void setAtributo(Object objeto, String nombreAtributo, Object valor) throws Exception {
        Method metodo = new PropertyDescriptor(nombreAtributo, objeto.getClass()).getWriteMethod();
        if (metodo == null) {
            throw new Exception("No hay un método setter para el atributo " + nombreAtributo);
        }
        Class[] tipos = metodo.getParameterTypes();
        if (tipos.length != 1) {
            throw new Exception("El método " + metodo.getName() + " tiene mas de un parámetro");
        }
        Class tipo = tipos[0];
        Object valorTipado = convertir(tipo, valor);

        metodo.invoke(objeto, valorTipado);
    }

    private static Object convertir(Class tipo, Object valor) {

        if (valor == null) {
            return null;
        }

        if (valor.getClass().equals(tipo) || tipo.isAssignableFrom(valor.getClass())) {
            return valor;
        }
        String valorString = valor.toString();
        Object valorConvertido = null;

        if (tipo == int.class || tipo == Integer.class) {
            valorConvertido = Conversion.toInteger(valorString);
        } else if (tipo == long.class || tipo == Long.class) {
            valorConvertido = Conversion.toLong(valorString);
        } else if (tipo == float.class || tipo == Float.class) {
            valorConvertido = Conversion.toFloat(valorString);
        } else if (tipo == double.class || tipo == Double.class) {
            valorConvertido = Conversion.toDouble(valorString);
        } else if (tipo == boolean.class || tipo == Boolean.class) {
            valorConvertido = Conversion.toBoolean(valorString);
        } else if (tipo == Color.class) {
            valorConvertido = Conversion.toColor(valorString);
        } else {
            valorConvertido = valorString;
        }

        if (tipo.isEnum()) {
            Object[] constantes = tipo.getEnumConstants();
            for (int i = 0; i < constantes.length; i++) {
                if (constantes[i].toString().equals(valorString)) {
                    return constantes[i];
                }
            }
            return null;
        }
        return valorConvertido;
    }

    public static Object crearObjeto(Class clazz) throws IllegalAccessException, InstantiationException {
        return clazz.newInstance();
    }

    public static Class crearClase(String nombreClase, String ruta, String codigoFuente) {
        FileWriter writer = null;
        StandardJavaFileManager fileManager = null;
        try {
            File sourceFile = new File(ruta + "/" + nombreClase + ".java");
            writer = new FileWriter(sourceFile);
            writer.write(codigoFuente);
            writer.close();
            writer = null;

            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            fileManager = compiler.getStandardFileManager(null, null, null);
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(new File(ruta)));

            // Se compila el archivo
            CompilationTask task = compiler.getTask(null, fileManager, null, null, null,
                    fileManager.getJavaFileObjectsFromFiles(Arrays.asList(sourceFile)));
            if (task == null) {
                return null;
            }
            task.call();

            File directorio = new File(ruta);
            URL[] urls = {directorio.toURI().toURL()};
            URLClassLoader classLoader = new URLClassLoader(urls);

            return Class.forName(nombreClase, true, classLoader);
            //Object iClass = thisClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
                if (fileManager != null) {
                    fileManager.close();
                }
            } catch (IOException ex) {
            }
        }
        return null;
    }

    /**
     * Conviete un objeto en un mapa con una entrada por cada atributo.
     *
     * @param objeto El objeto
     * @return El mapa
     */
    public static Map<String, Object> objetoAMapa(Object objeto) {
        if (objeto == null) {
            return null;
        }

        Map<String, Object> mapa = new LinkedHashMap<>();

        BeanInfo info;
        try {
            info = Introspector.getBeanInfo(objeto.getClass());
        } catch (IntrospectionException ex) {
            return null;
        }
        for (PropertyDescriptor property : info.getPropertyDescriptors()) {
            if (property.getReadMethod() != null && !"class".equals(property.getName())) {
                Object valor;
                try {
                    valor = property.getReadMethod().invoke(objeto);
                } catch (Exception ex) {
                    valor = null;
                }
                mapa.put(property.getName(), valor);
            }
        }
        return mapa;
    }
    /**
     * Aplica los valor de una mapa sobre los atribitos de un objeto javabean.
     */
    public static void mapaAObjeto(Map<String, Object> mapa, Object objeto) throws Exception {

        if (objeto == null) {
            return;
        }
        BeanInfo info;
        info = Introspector.getBeanInfo(objeto.getClass());
        for (PropertyDescriptor property : info.getPropertyDescriptors()) {
            if (property.getWriteMethod() != null && !"class".equals(property.getName())) {
                Object valor = mapa.get(property.getName());
                setAtributo(objeto, property.getName(), valor);
            }
        }
    }
}
