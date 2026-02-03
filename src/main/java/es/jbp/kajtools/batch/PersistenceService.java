package es.jbp.kajtools.batch;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

@Service
public class PersistenceService {

    private static final String FILE_NAME = "persistence.properties";

    private Properties properties;

    @PostConstruct
    public void init() {
        properties = new Properties();

        try (var inputStream = new FileInputStream(FILE_NAME)) {
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<String, String> extractProperties(String parent) {
        return properties.keySet().stream()
                .map(Objects::toString)
                .filter(k -> k.startsWith(parent + "."))
                .collect(Collectors.toMap(k -> k.substring(parent.length() + 1), k -> properties.getProperty(k)));
    }

    public void forceProperties(String parent, Map<String, String> valueMap) {
        valueMap.keySet()
                .forEach(k -> properties.setProperty(parent + "." + k, valueMap.get(k)));
        save();
    }

    public void save() {
        try (var fos = new FileOutputStream(FILE_NAME)) {
            properties.store(fos, "Updated " + LocalDateTime.now());
            fos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getPropertiesText(String parent, List<String> properties) {
        var savedProperties = extractProperties(parent);
        return properties
                .stream()
                .map(p -> p + " = " + savedProperties.getOrDefault(p, ""))
                .collect(Collectors.joining("\n"));
    }
}
