package es.jbp.kajtools.util;

import java.lang.reflect.Method;

public class AvroUtils {

  public static String extractAvroSchema(Object object) {
    if (object == null) {
      return "";
    }
    Class clazz = object.getClass();
    Method buildMethod;
    try {
      buildMethod = clazz.getMethod("getSchema");
    } catch (Exception ex) {
      return "";
    }
    Object schema;
    try {
      schema = buildMethod.invoke(object);
    } catch (Exception ex) {
      return "";
    }

    if (schema instanceof org.apache.avro.Schema) {
      return ((org.apache.avro.Schema) schema).toString();
    } else {
      return "";
    }
  }
}
