package es.jbp.kajtools.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public abstract class SerializationUtils {

    public InputStream getResourceStream(String resourceName) {
        return SerializationUtils.class.getClassLoader().getResourceAsStream(resourceName);
    }

    public String getResourceString(String resourceName) throws IOException {
        InputStream inputStream = getResourceStream(resourceName);
        if (inputStream == null) {
            return null;
        }
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    public <T> T deserializeFromString(String jsonString, TypeReference<T> valueType) throws IOException {
        return getObjectMapper().readValue(jsonString, valueType);
    }

    public <T> T deserializeFromString(String jsonString, Class<T> valueType) throws IOException {
        return getObjectMapper().readValue(jsonString, valueType);
    }

    public <T> T deserializeFromResource(String resourceName, TypeReference<T> valueType) throws IOException {
        InputStream inputStream = getResourceStream(resourceName);
        if (inputStream == null) {
            return null;
        }
        return getObjectMapper().readValue(inputStream, valueType);
    }

    public <T> T deserializeFromResource(String resourceName, Class<T> valueType) throws IOException {
        InputStream inputStream = getResourceStream(resourceName);
        if (inputStream == null) {
            return null;
        }
        return getObjectMapper().readValue(inputStream, valueType);
    }

    public <T> T deserializeFromFile(File file, Class<T> valueType) throws IOException {
        InputStream inputStream = new FileInputStream(file);
        return getObjectMapper().readValue(inputStream, valueType);
    }


    public String serialize(Object object) throws JsonProcessingException {
        return getObjectWriter().writeValueAsString(object);
    }

    public String serializeOrNull(Object object) {
        try {
            return getObjectWriter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            System.err.println(e.getMessage());
            return null;
        }
    }

    public void saveToFile(Object object, File file) throws IOException {
        getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(file, object);
    }

    public abstract ObjectMapper getObjectMapper();

    public ObjectWriter getObjectWriter() {
        return getObjectMapper().writerWithDefaultPrettyPrinter();
    }
}

