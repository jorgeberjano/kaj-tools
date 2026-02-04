package es.jbp.kajtools.batch;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PersistenceService {

    private static final String FILE_NAME = "./persistence.properties";

    private Properties properties;

    @PostConstruct
    public void init() {
        properties = new Properties();

        var path = Paths.get(FILE_NAME);

        try {
            var parentPath = path.getParent();
            if (!parentPath.toString().isEmpty()) {
                Files.createDirectories(parentPath);
            }
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
        } catch (IOException e) {
            log.error("No se pudo crear el archivo de persistencia");
        }

        try (var inputStream = new FileInputStream(FILE_NAME)) {
            properties.load(inputStream);
        } catch (IOException e) {
            log.error("No se pudo cargar el archivo de persistencia");
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
            log.error("No se pudo guardar el archivo de persistencia", e);
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
