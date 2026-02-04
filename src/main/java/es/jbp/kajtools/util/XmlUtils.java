package es.jbp.kajtools.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

public class XmlUtils extends SerializationUtils {

    public static final XmlUtils instance = new XmlUtils();

    private final XmlMapper mapper;

    private XmlUtils() {
        mapper = new XmlMapper();
    }

    @Override
    public ObjectMapper getObjectMapper() {
        return mapper;
    }

    public String formatXml(String xml) {
        if (xml == null) {
            return null;
        }

        try {
            JsonNode jsonNode = getObjectMapper().readTree(xml);
            return serialize(jsonNode);
        } catch (JsonProcessingException e) {
            return xml;
        }
    }
}
