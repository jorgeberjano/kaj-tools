package es.jbp.kajtools.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import java.io.IOException;
import org.apache.commons.lang3.StringUtils;

public class JsonUtils {

    private static ObjectMapper getObjectMapper() {
        return new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
            .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
    }

    public static <T> T stubFromJson(String json, Class<T> valueType) throws IOException {
        return getObjectMapper().readValue(json, valueType);
    }

    public static <T> T createFromJson(String json, TypeReference<T> valueType) throws IOException {
        return getObjectMapper().readValue(json, valueType);
    }

    public static String jsonToTemplate(String json) {
        return JsonTemplateCreator.fromJson(json).create();
    }

    public static boolean isArray(String json) {
        return json.trim().startsWith("[");
    }

    public static boolean isTemplate(String json) {
        // Se considera que es un template cuando tiene mas @ y $ que dobles comillas.
        // TODO: ver si hay una forma mas exacta
        return StringUtils.countMatches(json, "@")
            + StringUtils.countMatches(json, "$") > StringUtils.countMatches(json, "\"");
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
}
