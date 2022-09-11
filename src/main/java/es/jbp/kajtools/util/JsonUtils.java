package es.jbp.kajtools.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.util.Map;

public class JsonUtils {

  private JsonUtils() {
  }

  private static ObjectMapper getObjectMapper() {
    return new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
        .configure(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS, false)
        .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
        .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);

  }

  public static <T> T createFromJson(String json, Class<T> valueType) throws IOException {
    return getObjectMapper().readValue(json, valueType);
  }

  public static <T> T createFromJson(String json, TypeReference<T> valueType) throws IOException {
    return getObjectMapper().readValue(json, valueType);
  }

  public static String toJson(Object object) throws JsonProcessingException {
    return getObjectMapper().writeValueAsString(object);
  }

  public static boolean isArray(String json) {
    return json.trim().startsWith("[");
  }

  public static String formatJson(String json) {
    if (json == null) {
      return null;
    }
    Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .serializeNulls()
        .create();
    try {
      JsonElement jsonElement = gson.fromJson(json, JsonElement.class);
      return gson.toJson(jsonElement);
    } catch (Exception e) {
      return json;
    }
  }

  public static Map<String, Object> toMap(String json) {
    Gson gson = new Gson();
    return gson.fromJson(json, new TypeToken<Map<String, Object>>() {
    }.getType());
  }
}
