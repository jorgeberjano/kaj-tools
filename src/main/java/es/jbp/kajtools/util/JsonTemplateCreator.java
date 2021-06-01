package es.jbp.kajtools.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.commons.lang3.StringUtils;

public class JsonTemplateCreator {

  private static final String START_OBJECT = "{\r\n";
  private static final String END_OBJECT = "}";
  private static final String START_ARRAY = "[\r\n";
  private static final String END_ARRAY = "]";
  private static final String NEXT_ELEMENT = ",\r\n";
  private static final String LAST_ELEMENT = "\r\n";
  private static final String INDENTATION = "  ";
  private static final String VALUE_SEPARATOR = ": ";

  private String json;
  

  public static JsonTemplateCreator fromJson(String json) {
    return new JsonTemplateCreator(json);
  }

  public JsonTemplateCreator(String json) {
    this.json = json;
  }

  public String create() {
    JsonObject jsonObject = new Gson().fromJson(json, JsonObject.class);
    return createObject(jsonObject, 1);
  }

  private String createElement(JsonElement jsonElement, int indentation) {

    if (jsonElement.isJsonArray()) {
      return createArray(jsonElement.getAsJsonArray(), indentation);
    } else if (jsonElement.isJsonObject()) {
      return createObject(jsonElement.getAsJsonObject(), indentation);
    } else if (jsonElement.isJsonPrimitive()) {
      return createPrimitive(jsonElement.getAsJsonPrimitive());
    } else {
      return "null";
    }
  }

  private String createPrimitive(JsonPrimitive jsonPrimitive) {

    String value = jsonPrimitive.getAsString();

    StringBuilder builder = new StringBuilder();
    if (value.contains(":")) {
      builder.append("@raw");
      value = "`\"" + value + "\"`";
    } else if (jsonPrimitive.isBoolean()) {
      builder.append("@b");
    } else if (jsonPrimitive.isNumber()) {
      builder.append(value.contains(".") ? "@f" : "@i");
    } else if (jsonPrimitive.isString()) {
      builder.append("@s");
    } else {
      builder.append("@smart");
    }
    builder.append("(");
    builder.append(value);
    builder.append(")");
    return builder.toString();
  }

  private String createArray(JsonArray jsonArray, int indentation) {
    StringBuilder builder = new StringBuilder();
    builder.append(START_ARRAY);

    for (int i = 0, n = jsonArray.size(); i < n; i++) {
      builder.append(StringUtils.repeat(INDENTATION, indentation));
      String content = createElement(jsonArray.get(i), indentation + 1);
      builder.append(content);
      if (i < n - 1) {
        builder.append(NEXT_ELEMENT);
      }
    }
    builder.append(LAST_ELEMENT);
    builder.append(StringUtils.repeat(INDENTATION, indentation - 1));
    builder.append(END_ARRAY);
    return builder.toString();
  }

  private String createObject(JsonObject jsonObject, int indentation) {
    StringBuilder builder = new StringBuilder();
    builder.append(START_OBJECT);
    boolean first = true;
    for (String key : jsonObject.keySet()) {
      if (!first) {
        builder.append(NEXT_ELEMENT);
      } else {
        first = false;
      }
      builder.append(StringUtils.repeat(INDENTATION, indentation));
      builder.append(key);
      builder.append(VALUE_SEPARATOR);
      String content = createElement(jsonObject.get(key), indentation + 1);
      builder.append(content);
    }
    builder.append(LAST_ELEMENT);
    builder.append(StringUtils.repeat(INDENTATION, indentation - 1));
    builder.append(END_OBJECT);
    return builder.toString();
  }
}
