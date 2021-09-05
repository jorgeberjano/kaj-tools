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

    private static ObjectMapper getObjectMapper() {
        return new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
            .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
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
        JsonElement jsonElement = gson.fromJson(json, JsonElement.class);
        return gson.toJson(jsonElement);
    }

  public static Map<String, Object> toMap(String json) {
      Gson gson = new Gson();
      return gson.fromJson(json, new TypeToken<Map<String, Object>>(){}.getType());
  }
}
