package es.jbp.kajtools.reflexion;

import java.awt.Color;
import java.math.BigDecimal;
import java.sql.Blob;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

/**
 * Utilidades para conversión.
 *
 * @author jberjano
 */
public class Conversion {

  private static final Set conjuntoCadenasTrue;
  private static final Set conjuntoCadenasFalse;
  private static char chMiles = 0;
  private static char chDecimal = '.';

  private static char chDecimalLocal = ',';

  static {
    String[] cadenasTrue = {"si", "sí", "true", "verdadero", "t", "v"};
    conjuntoCadenasTrue = new HashSet(Arrays.asList(cadenasTrue));

    String[] cadenasFalse = {"no", "false", "falso", "f"};
    conjuntoCadenasFalse = new HashSet(Arrays.asList(cadenasFalse));
  }

  public static Integer toInteger(Object obj) {
    if (obj == null) {
      return null;
    }
    if (obj instanceof Integer) {
      return (Integer) obj;
    }
    if (obj instanceof Long) {
      return ((Long) obj).intValue();
    }
    if (obj instanceof Double) {
      return ((Double) obj).intValue();
    }
    if (obj instanceof Float) {
      return ((Float) obj).intValue();
    }

    try {
      return Integer.parseInt(obj.toString());
    } catch (Throwable e) {
    }

    try {
      return Integer.decode(obj.toString());
    } catch (Exception e) {
      return null;
    }
  }

  public static Long toLong(Object obj) {
    if (obj == null) {
      return null;
    }
    if (obj instanceof Long) {
      return (Long) obj;
    }
    if (obj instanceof Integer) {
      return ((Integer) obj).longValue();
    }
    if (obj instanceof Double) {
      return ((Double) obj).longValue();
    }
    if (obj instanceof Float) {
      return ((Float) obj).longValue();
    }

    try {
      return Long.parseLong(obj.toString());
    } catch (Throwable e) {
    }

    try {
      return Long.decode(obj.toString());
    } catch (Throwable e) {
    }

    return null;
  }

  public static Float toFloat(Object obj) {
    if (obj == null) {
      return null;
    }
    if (obj instanceof Long) {
      return ((Long) obj).floatValue();
    }
    if (obj instanceof Integer) {
      return ((Integer) obj).floatValue();
    }
    if (obj instanceof Double) {
      return ((Double) obj).floatValue();
    }
    if (obj instanceof Float) {
      return (Float) obj;
    }

    try {
      return Float.parseFloat(obj.toString());
    } catch (Throwable e) {
      return null;
    }
  }

  public static Double toDouble(Object obj) {
    if (obj == null) {
      return null;
    }
    if (obj instanceof Long) {
      return ((Long) obj).doubleValue();
    }
    if (obj instanceof Integer) {
      return ((Integer) obj).doubleValue();
    }
    if (obj instanceof Double) {
      return (Double) obj;
    }
    if (obj instanceof Float) {
      return ((Float) obj).doubleValue();
    }

    try {
      return Double.parseDouble(obj.toString());
    } catch (Throwable e) {
      return null;
    }
  }

  public static BigDecimal toBigDecimal(Object obj) {
    if (obj == null) {
      return null;
    }
    if (obj instanceof Long) {
      return new BigDecimal((Long) obj);
    }
    if (obj instanceof Integer) {
      return new BigDecimal((Integer) obj);
    }
    if (obj instanceof Double) {
      return new BigDecimal((Double) obj);
    }
    if (obj instanceof Float) {
      return new BigDecimal((Float) obj);
    }

    try {
      return new BigDecimal(obj.toString());
    } catch (Throwable e) {
      return null;
    }
  }

  public static Boolean toBoolean(Object obj) {
    if (obj == null) {
      return null;
    }
    if (obj instanceof Boolean) {
      return (Boolean) obj;
    }

    String texto = obj.toString().trim().toLowerCase();

    if (conjuntoCadenasTrue.contains(texto)) {
      return true;
    }
    if (conjuntoCadenasFalse.contains(texto)) {
      return false;
    }

    Integer entero = toInteger(obj);
    if (entero != null) {
      return entero != 0;
    }
    return null;
  }

  public static String toString(Object valor) {
    if (valor == null) {
      return null;
    } else if (valor instanceof String) {
      return (String) valor;
    } else {
      return valor.toString();
    }
  }

  public static byte[] toByteArray(Object valor) {
    if (valor == null) {
      return null;
    } else if (valor instanceof byte[]) {
      return (byte[]) valor;
    } else if (valor instanceof String) {
      return java.util.Base64.getDecoder().decode((String) valor);
    } else {
      return valor.toString().getBytes();
    }
  }

  public static byte[] toBytes(Object valor) throws Exception {
    if (valor == null) {
      return null;
    }
    if (valor instanceof byte[]) {
      return (byte[]) valor;
    }
    if (valor instanceof Blob) {
      Blob blob = (Blob) valor;
      return blob.getBytes(1, (int) blob.length());
    }
    if (valor instanceof String) {
      return ((String) valor).getBytes("UTF-8");
    }
    return null;
  }


  public static byte[] hexToBytes(String s) {
    int len = s.length();
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
          + Character.digit(s.charAt(i + 1), 16));
    }
    return data;
  }

  final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

  public static String bytesToHex(byte[] bytes) {
    int length = bytes.length;
    char[] hexChars = new char[length * 2];
    for (int j = 0; j < length; j++) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 2] = hexArray[v >>> 4];
      hexChars[j * 2 + 1] = hexArray[v & 0x0F];
    }
    return new String(hexChars);
  }

  public static String bytesToHex(byte[] bytes, int length) {
    char[] hexChars = new char[length * 2];
    for (int j = 0; j < length; j++) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 2] = hexArray[v >>> 4];
      hexChars[j * 2 + 1] = hexArray[v & 0x0F];
    }
    return new String(hexChars);
  }

  /**
   * Convierte un byte en una cadena con su valor hexadecimal.
   *
   * @param octeto
   * @return
   */
  public static String byteToHex(byte octeto) {
    char[] hexChars = new char[2];
    int v = octeto & 0xFF;
    hexChars[0] = hexArray[v >>> 4];
    hexChars[1] = hexArray[v & 0x0F];
    return new String(hexChars);
  }

  public static String stringToHexString(String str) {
    StringBuilder hex = new StringBuilder();
    for (int i = 0; i < str.length(); i++) {
      hex.append(Integer.toHexString(str.charAt(i)));
    }
    return hex.toString();
  }

  public static String hexStringToString(String str) {
    byte[] txtInByte = new byte[str.length() / 2];
    int j = 0;
    for (int i = 0; i < str.length(); i += 2) {
      txtInByte[j++] = Byte.parseByte(str.substring(i, i + 2), 16);
    }
    String result = new String(txtInByte);
    int endpos = result.indexOf('\0');
    if (endpos != -1) {
      result = result.substring(0, endpos);
    }

    return result;
  }

  public static boolean equals(Object o1, Object o2) {
    if (o1 == o2) {
      return true;
    }

    if (o1 == null || o2 == null) {
      return false;
    }

    return o1.equals(o2);
  }

  public static long ipALong(String ip) {
    ip = ip.replace('/', ' ').trim();

    try {
      Scanner sc = new Scanner(ip).useDelimiter("\\.");
      return (sc.nextLong() << 24)
          + (sc.nextLong() << 16)
          + (sc.nextLong() << 8)
          + (sc.nextLong());
    } catch (Exception e) {
      return 0;
    }
  }

  public static String quitarAcentos(String input) {
    // Cadena de caracteres original a sustituir.
    String original = "áàäéèëíìïóòöúùuñÁÀÄÉÈËÍÌÏÓÒÖÚÙÜÑçÇ";
    // Cadena de caracteres ASCII que reemplazarán los originales.
    String ascii = "aaaeeeiiiooouuunAAAEEEIIIOOOUUUNcC";
    String output = input;
    for (int i = 0; i < original.length(); i++) {
      output = output.replace(original.charAt(i), ascii.charAt(i));
    }
    return output;
  }

  public static String capitalizar(String texto) {
    if (texto == null || texto.isEmpty()) {
      return texto;
    }
    String textoCapitalizado = texto.substring(0, 1).toUpperCase();
    if (texto.length() > 1) {
      textoCapitalizado += texto.substring(1);
    }
    return textoCapitalizado;
  }

  public static List<String> convertirTextoEnLista(String texto) {
    return convertirTextoEnLista(texto, ",");
  }

  public static List<String> convertirTextoEnLista(String texto, String separador) {
    if (StringUtils.isBlank(texto)) {
      return new ArrayList();
    }
    String elementos[] = texto.split(separador);
    return Arrays.asList(elementos).stream().map((elemento) -> {
      return elemento.trim();
    }).collect(Collectors.toList());
  }

  /**
   * Convierte una lista de objetos en un texto con la representación de texto de cada uno de los objetos usando el
   * separador coma.
   */
  public static String convertirListaEnTexto(List lista) {
    return convertirListaEnTexto(lista, ",");
  }

  /**
   * Convierte una lista de objetos en un texto con la representación de texto de cada uno de los objetos usando un
   * separador determinado.
   */
  public static String convertirListaEnTexto(List lista, String separador) {
    if (lista == null) {
      return null;
    }
    StringBuilder builder = new StringBuilder();
    boolean primero = true;
    for (Object elemento : lista) {
      String texto = Conversion.toString(elemento);
      if (!primero) {
        builder.append(separador);
      } else {
        primero = false;
      }
      builder.append(texto);
    }
    return builder.toString();
  }

  /**
   * Concadena dos cadenas separandolas por un separador en el caso de que ambas sean no vacias.
   */
  public static String concatenar(String cadena1, String separador, String cadena2) {

    boolean tieneValorCadena1 = cadena1 != null && !cadena1.isEmpty();
    boolean tieneValorCadena2 = cadena2 != null && !cadena2.isEmpty();

    if (!tieneValorCadena1 && !tieneValorCadena2) {
      return "";
    } else if (!tieneValorCadena1) {
      return cadena2;
    } else if (!tieneValorCadena2) {
      return cadena1;
    } else {
      return cadena1 + separador + cadena2;
    }
  }

  public static final String LETRAS_NIF = "TRWAGMYFPDXBNJZSQVHLCKE";

  /**
   * Calcula la letra del NIF que le corresponde a un DNI expresado como una cadena.
   */
  public static char calcularLetraNIF(String dni) {
    return calcularLetraNIF(Conversion.toInteger(dni));
  }

  /**
   * Calcula la letra del NIF que le corresponde a un DNI expresado como un entero.
   */
  public static char calcularLetraNIF(int dni) {
    return LETRAS_NIF.charAt(dni % 23);
  }

  public static String extraerSubstring(String texto, int inicio, int fin) {
    int longitud = fin - inicio;
    if (texto == null || longitud <= 0 || inicio < 0 || inicio >= texto.length()) {
      return "";
    }
    if (fin < texto.length()) {
      return texto.substring(inicio, fin);
    } else {
      return texto.substring(inicio);
    }
  }

  public static String formatearBigDecimal(final Object cantidad, final int decimales) {
    BigDecimal valor = toBigDecimal(cantidad);
    if (valor == null) {
      return "";
    }
    valor = valor.setScale(decimales, BigDecimal.ROUND_HALF_DOWN);
    return valor.toString();
  }

  public static String formatearReal(String strFormato, final Double cantidad) {
    String strResultado = "";

    if (StringUtils.isBlank(strFormato)) {
      strFormato = "%f";
    }

    int nPre, nPost;
    // Se calculan el número de caracteres anteriores (nPre) al formato
    if ((nPre = strFormato.indexOf('%')) == -1) {
      return strFormato;
    }
    // Se calculan el número de caracteres (nFormato) del formato
    if ((nPost = strFormato.substring(nPre).indexOf('f')) == -1) {
      return strFormato;
    }

    String strPre = strFormato.substring(0, nPre);
    String strFrmt = strFormato.substring(nPre, nPost + 1);
    String strPost = strFormato.substring(nPost + 1);

    // Si el formato no incluye el punto se suprimen los ceros de la derecha de los decimales
    boolean bQuitarCeros = strFrmt.indexOf('.') == -1;

    // Se incluyen los caracteres anteriores al formato
    strResultado += strPre;

    // Se formatea el número
    String strNumero = String.format(strFrmt, cantidad);

    // Si hay espacios a la izquierda se pasan al resultado
    for (int i = 0; i < strNumero.length(); i++) {
      if (strNumero.charAt(i) == ' ') {
        strResultado += ' ';
      } else {
        break;
      }
    }
    strNumero = strNumero.trim();

    int posicionDecimal = strNumero.indexOf(chDecimalLocal);
    String strParteEntera = strNumero.substring(0, posicionDecimal);
    String strParteDecimal = strNumero.substring(posicionDecimal + 1);

    int cifrasEnteras = strParteEntera.length();
    if (cifrasEnteras > 3 && chMiles != 0) {
      if (cifrasEnteras % 3 > 0) {
        strResultado += strNumero.substring(0, cifrasEnteras % 3) + chMiles;
      }

      for (int i = cifrasEnteras % 3; i < cifrasEnteras; i += 3) {
        strResultado += strNumero.substring(i, 3);
        if (i + 3 < cifrasEnteras) {
          strResultado += chMiles;
        }
      }
    } else {
      strResultado += strParteEntera;
    }

    if (bQuitarCeros) {
      while (!strParteDecimal.isEmpty() && strParteDecimal.charAt(strParteDecimal.length() - 1) == '0') {
        strParteDecimal = strParteDecimal.substring(1);
      }
    }
    if (!strParteDecimal.isEmpty()) {
      strResultado += chDecimal + strParteDecimal;
    }
    // Se incluyen los caracteres posteriores al formato
    strResultado += strPost;

    return strResultado;
  }


  public static Color toColor(String valorString) {
    try {
      return Color.decode(valorString);
    } catch (Exception ex) {
      return null;
    }
  }

  public static Object toObject(String texto, Class clazz) {
    if (clazz == Integer.class || clazz == int.class) {
      return toInteger(texto);
    } else if (clazz == Long.class || clazz == long.class) {
      return toLong(texto);
    } else if (clazz == Double.class || clazz == double.class) {
      return toDouble(texto);
    } else if (clazz == Float.class || clazz == float.class) {
      return toFloat(texto);
    } else if (clazz == String.class) {
      return texto;
    } else if (clazz == Color.class) {
      return toColor(texto);
    } else if (clazz.isEnum()) {
      Object[] constantes = clazz.getEnumConstants();
      for (Object constante : constantes) {
        if (constante.toString().equals(texto)) {
          return constante;
        }
      }
    }
    return null;
  }

  public static String primeraMayusculas(String texto) {
    if (StringUtils.isBlank(texto)) {
      return texto;
    }
    return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
  }

}
