package es.jbp.kajtools.configuration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import es.jbp.kajtools.Environment;
import es.jbp.kajtools.util.ResourceUtil;
import java.io.IOException;
import java.util.List;
import lombok.Getter;

public class Configuration {

  @Getter
  private static List<Environment> environmentList;

  @Getter
  private static String variablesProperties;

  private Configuration() {
  }

  public static void load() throws IOException {
    loadEnvironments();
    loadVariablesProperties();
  }

  public static void loadEnvironments() throws IOException {
    environmentList = new ObjectMapper(new YAMLFactory())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .readValue(Configuration.class.getResourceAsStream("/environments.yml"),
            new TypeReference<List<Environment>>() {
            });
  }

  public static void loadVariablesProperties() throws IOException {
    variablesProperties = ResourceUtil.readResourceString("variables.properties");
  }
}
