package es.jbp.kajtools.util;

import java.util.Collection;
import java.util.HashSet;
import org.apache.commons.lang3.StringUtils;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

/**
 * Clase para crear objetos en profundidad con valores arbitrarios
 * @author Jorge Berjano
 */
public class DeepTestObjectCreator {

    private final Stack<String> processedClasses = new Stack<>();

    private final List<String> errors = new ArrayList<>();
    private static final int ELEMENT_COUNT = 2;

    public void init() {
        errors.clear();
    }

    public Object createObject(String className, String attributeName) {
        return createObject(classForName(className), attributeName);
    }

    public Object createObject(Class clazz, String attributeName) {
        if (clazz == null) {
            return null;
        }
        if (processedClasses.contains(clazz.getName())) {
            if (!StringUtils.isBlank(attributeName)) {
                reportError("El atributo " + attributeName + " de tipo " + clazz.getName() + " produce una referencia cíclica", null);
            } else {
                reportError("La clase " + clazz.getName() + " produce una referencia cíclica", null);
            }
            return null;
        }
        processedClasses.push(clazz.getName());
        try {
            if (String.class.equals(clazz)) {
                return createStringByName(attributeName);
            } else if (int.class.equals(clazz) || Integer.class.equals(clazz)) {
                return 9;
            } else if (long.class.equals(clazz) || Long.class.equals(clazz)) {
                return 9L;
            } else if (double.class.equals(clazz) || Double.class.equals(clazz)) {
                return 9.9;
            } else if (float.class.equals(clazz) || Float.class.equals(clazz)) {
                return 9.9F;
            } else if (byte.class.equals(clazz) || Byte.class.equals(clazz)) {
                return 'A';
            } else if (short.class.equals(clazz) || Short.class.equals(clazz)) {
                return 9;
            } else if (boolean.class.equals(clazz) || Boolean.class.equals(clazz)) {
                return false;
            } else if (java.math.BigDecimal.class.equals(clazz)) {
                return new java.math.BigDecimal(99);
            } else if (clazz.isEnum()) {
                return clazz.getEnumConstants()[0];
            } else if (isAvroGenerated(clazz)) {
                return createAvroObject(clazz, attributeName);
            } else {
                return createBeanObject(clazz, attributeName);
            }
        } finally {
            processedClasses.pop();
        }
    }

    public Object createAvroObject(Class clazz, String attributeName) {
        if (clazz == null) {
            return null;
        }

        if (!isAvroGenerated(clazz)) {
            return null;
        }

        Method methodNewBuilder;
        Object builderObject;
        try {
            methodNewBuilder = clazz.getMethod("newBuilder");
        } catch (Throwable ex) {
            reportError("La clase " + clazz.getName() + " no tiene builder", ex);
            return null;
        }
        try {
            builderObject = methodNewBuilder.invoke(null);
        } catch (Throwable ex) {
            reportError("Error al invocar al builder de La clase " + clazz.getName(), ex);
            return null;
        }
        return createObjectFromBuilder(builderObject);
    }

    private Object createBeanObject(Class clazz, String attributeName) {
        Object object = instantiateClassWithDefaultConstructor(clazz, attributeName);
        if (object == null) {
            object = instantiateClassWithAnyConstructor(clazz);
        }

        if (object != null) {
            injectAttributeValues(object);
        }
        return object;
    }

    private Object createObjectFromBuilder(Object builderObject) {

        injectAttributeValues(builderObject);

        Class builderClass = builderObject.getClass();

        Method buildMethod;
        try {
            buildMethod = builderClass.getMethod("build");
        } catch (Exception ex) {
            reportError("El builder " + builderClass.getName() + " no tiene método build()", ex);
            return null;
        }
        Object object;
        try {
            object = buildMethod.invoke(builderObject);
        } catch (Exception ex) {
            reportError("Error al invocar al metodo build() de " + builderClass.getName(), ex.getCause() != null ? ex.getCause() : ex);
            return null;
        }
        return object;
    }

    private void injectAttributeValues(Object beanObject) throws SecurityException {
        Class beanClass = beanObject.getClass();
        Method[] methods = beanClass.getMethods();
        for (Method setterMethod : methods) {
            if (!isSetter(setterMethod)) {
                // No es un atributo con get/set
                continue;
            }
            String attributeName = setterMethod.getName().substring(3);
            String getterName = "get" + attributeName;
            Method getterMethod;
            try {
                getterMethod = beanClass.getMethod(getterName);
            } catch (Exception ex) {
                // No es un atributo con get/set
                continue;
            }
            injectAttributeValue(beanObject, getterMethod, setterMethod);
        }
    }

    private Object injectAttributeValue(Object object, Method getterMethod, Method setterMethod) {
        Class returnType = getterMethod.getReturnType();

        try {
            // Si se trata de un RecordBuilder y no de un atributo normal no se hace nada
            if (Class.forName("org.apache.avro.data.RecordBuilder").isAssignableFrom(returnType)) {
                return null;
            }
        } catch (ClassNotFoundException ex) {
            reportError("No se encuentra RecordBuilder en el classpath", ex);
        }
        Object value = createAtributeValue(returnType, getterMethod);
        if (value == null) {
            return null;
        }

        try {
            return setterMethod.invoke(object, value);
        } catch (Exception ex) {
            reportError("Error al invocar al método " + setterMethod.getName(), ex);
            return null;
        }
    }

    private boolean isSetter(Method method) {
        return method.getName().startsWith("set") && method.getParameterCount() == 1;
    }

    private Object createAtributeValue(Class clazz, Method getterMethod) {
        String attributeName = StringUtils.uncapitalize(getterMethod.getName().substring(3));

        Type type = getterMethod.getGenericReturnType();
        if (clazz.isArray()) {
            return createArrayObject(type, attributeName);
        }
        if (List.class.isAssignableFrom(clazz)) {
            return createListObject(type, attributeName);
        }
        if (Set.class.isAssignableFrom(clazz)) {
            return createSetObject(type, attributeName);
        }
        if (Map.class.isAssignableFrom(clazz)) {
            return createMapObject(type, attributeName);
        }
//        if (es.eci.firefly.commons.type.BigDecimal.class.equals(clazz)) {
//            return new es.eci.firefly.commons.type.BigDecimal(99.55);
//        }
//        if (es.eci.firefly.commons.type.Date.class.equals(clazz)) {
//            return new es.eci.firefly.commons.type.Date("2020-12-20T17:33Z");
//        }

        return createObject(clazz, attributeName);
    }

    private boolean isAvroGenerated(Class clazz) {
        Annotation annotationAvroGenerated = clazz.getAnnotation(org.apache.avro.specific.AvroGenerated.class);
        return annotationAvroGenerated != null;
    }

    private Class classForName(String className) {
        try {
            return Class.forName(className);
        } catch (Exception ex) {
            reportError("No existe la clase " + className, null);
            return null;
        }
    }

    private Map createMapObject(Type type, String attributeName) {
        Map list = new HashMap();
        Object key = createElementOfParametrizedType(type, 0, attributeName);
        Object value = createElementOfParametrizedType(type, 1, attributeName);
        if (key != null) {
            list.put(key, value);
        }
        return list;
    }

    private List createListObject(Type type, String attributeName) {
        return (List) populateCollection(new ArrayList(), type, attributeName);
    }

    private Set createSetObject(Type type, String attributeName) {
        return (Set) populateCollection(new HashSet(), type, attributeName);
    }

    private Collection populateCollection(Collection collection, Type type, String attributeName) {
        for (int i = 0; i < ELEMENT_COUNT; i++) {
            Object element = createElementOfParametrizedType(type, 0, attributeName);
            if (element != null) {
                collection.add(element);
            }
        }
        return collection;
    }

    private Object createArrayObject(Type type, String attributeName) {        
        if (type instanceof Class) {
            Class elementType = ((Class) type).getComponentType();
            Object array =  Array.newInstance(elementType, ELEMENT_COUNT);
            int length = Array.getLength(array);
            for (int i = 0; i < length; i++) {        
                Object element = createObject(elementType, attributeName + "[]");
                Array.set(array, i, element);
            }
            return array;
        } else {
            return null; // TODO: contemplar el resto de casos
        }
    }

    private Object createElementOfParametrizedType(Type type, int index, String attributeName) {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            String elementClassName = typeArguments[index].getTypeName();
            return createObject(elementClassName, attributeName);
        } else {
            return null;
        }
    }

    private String createStringByName(String name) {
        if (StringUtils.isBlank(name)) {
            return "test";
        }
        String[] splitArray = name.split("(?=\\p{Upper})");
        List<String> splitList = Arrays.stream(splitArray).map(e -> e.toLowerCase()).collect(Collectors.toList());
        Set<String> splitSet = Arrays.stream(splitArray).map(e -> e.toLowerCase()).collect(Collectors.toSet());

        if (splitSet.contains("date")) {
            return "2020-11-15T12:22Z";
        } else if (splitSet.contains("currency")) {
            return "EUR";
        } else if (splitSet.contains("price")) {
            return "9.99";
        } else if (splitSet.contains("discount")) {
            return "20";
        } else if (splitSet.contains("stock")) {
            return "9999";
        } else {
            String splitString = StringUtils.join(splitList, "-");
            return splitString.isEmpty() ? "test" : splitString + "-test";
        }
    }

    public String getJsonFromObject(Object obj) {
        if (obj == null) {
            return "{}";
        }

        if (isAvroGenerated(obj.getClass())) {
            String json = obj.toString();
            return JsonUtils.formatJson(json);
        }

//        Type bigDecimalType = new TypeToken<BigDecimal>() {}.getType();
//        JsonSerializer<BigDecimal> bigDecimalSerializer = new JsonSerializer<BigDecimal>() {
//            @Override
//            public JsonElement serialize(BigDecimal value, Type type,
//                JsonSerializationContext jsonSerializationContext) {
//                return new JsonPrimitive(value.getValue());
//            }
//        };
        Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setPrettyPrinting()
            .serializeNulls()
//            .registerTypeAdapter(bigDecimalType, bigDecimalSerializer)
            .create();
        return gson.toJson(obj);
    }

    public List<String> getErrors() {
        return errors;
    }

    public Object createObjectFromJson(String nombreClase, String json) {
        Class clazz;
        try {
            clazz = Class.forName(nombreClase);

        } catch (ClassNotFoundException ex) {
            reportError("No se encuentra la clase " + nombreClase, null);
            return null;
        }
        ObjectMapper objectMapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
            .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);

        Object object;
        try {
            object = objectMapper.readValue(json, clazz);
        } catch (IOException ex) {
            reportError("No se ha podido mapear el json en la clase " + nombreClase, ex);
            return null;
        }

        return object;
    }

    private Object instantiateClassWithAnyConstructor(Class clazz) {
        Constructor[] constructors;
        try {
            constructors = clazz.getConstructors();
        } catch (Exception ex) {
            reportError("Error al obtener los constructores de " + clazz.getName(), ex);
            return null;
        }

        if (constructors == null || constructors.length == 0) {
            try {
                return clazz.newInstance();
            } catch (InstantiationException ex) {
                reportError("Error al crear una instancia de la clase " + clazz.getName(), ex);
                return null;
            } catch (IllegalAccessException ex) {
                reportError("Acceso ilegal a la clase " + clazz.getName(), ex);
                return null;
            }
        }

        Constructor constructor = constructors[0];
        Class[] parameterClasses = constructor.getParameterTypes();
        Object[] parameters = Arrays.stream(parameterClasses).map(c -> createObject(c, null)).collect(Collectors.toList()).toArray();
        try {
            return constructor.newInstance(parameters);
        } catch (Exception ex) {
            reportError("Error al crear una instancia de la clase " + clazz.getName() + " con uno de sus constructores", ex);
            return null;
        }
    }

    private Object instantiateClassWithDefaultConstructor(Class clazz, String attributeName) {
        Constructor constructor;
        try {
            constructor = clazz.getConstructor();
        } catch (Exception ex) {
            //reportError("La clase " + clazz.getName() + " no tiene un constructor sin parámetros", ex);
            return null;
        }
        try {
            return constructor.newInstance();
        } catch (Exception ex) {
            if (StringUtils.isBlank(attributeName)) {
                reportError("Error al crear una instancia de la clase " + clazz.getName(), ex);
            } else {
                reportError("Error al crear una instancia de la clase " + clazz.getName() + " para el atributo " + attributeName, ex);
            }
            return null;
        }
    }

    private void reportError(String message, Throwable ex) {
        String errorMessage = message;
        if (ex != null) {
            errorMessage += ". Causa: [" + ex.getClass().getName() + "] " + ex.getMessage();
        }
        errors.add(errorMessage);
        System.err.println(errorMessage);
    }

    public String extractAvroSchema(Object object) {
        if (object == null) {
            return "";
        }
        Class clazz = object.getClass();
        if (!isAvroGenerated(clazz)) {
            return "";
        }
        Method buildMethod;
        try {
            buildMethod = clazz.getMethod("getSchema");
        } catch (Exception ex) {
            reportError("El objeto " + clazz.getName() + " no tiene método getSchema()", ex);
            return null;
        }
        Object schema;
        try {
            schema = buildMethod.invoke(object);
        } catch (Exception ex) {
            reportError("Error al invocar al método getSchema() de " + clazz.getName(), ex.getCause() != null ? ex.getCause() : ex);
            return null;
        }

        if (schema instanceof org.apache.avro.Schema) {
            return JsonUtils.formatJson(((org.apache.avro.Schema) schema).toString());
        } else {
            reportError("El método getSchema() de " + clazz.getName() + " no devuelve un objeto de clase Schema", null);
            return "";
        }
    }
}
