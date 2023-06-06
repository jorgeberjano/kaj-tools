package es.jbp.kajtools.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class YamlUtils extends SerializationUtils {

    public static final YamlUtils instance = new YamlUtils();

    @Override
    public ObjectMapper getObjectMapper() {
        return new ObjectMapper(new YAMLFactory());
    }
}
