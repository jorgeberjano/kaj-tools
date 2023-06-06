package es.jbp.kajtools.util;

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

}
