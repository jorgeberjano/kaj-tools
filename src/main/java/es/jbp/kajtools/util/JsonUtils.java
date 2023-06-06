package es.jbp.kajtools.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.Map;

public class JsonUtils extends SerializationUtils {

    public static final JsonUtils instance = new JsonUtils();
    private ObjectMapper mapper;

    public void setObjectMapper(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public static String formatJson(String json) {
        return instance.format(json);
    }

    public static boolean isArray(String json) {
        return json.trim().startsWith("[");
    }

    private JsonUtils() {
        mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public ObjectMapper getObjectMapper() {
        return mapper;
    }

    public ObjectWriter getObjectWriter() {
        return mapper.writerWithDefaultPrettyPrinter();
    }

    public String format(String json) {
        if (json == null) {
            return null;
        }

        try {
            JsonNode jsonNode = getObjectMapper().readTree(json);
            return serialize(jsonNode);
        } catch (JsonProcessingException e) {
            return json;
        }
    }

    public Map<String, Object> toMap(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, new TypeToken<Map<String, Object>>() {
        }.getType());
    }

}
