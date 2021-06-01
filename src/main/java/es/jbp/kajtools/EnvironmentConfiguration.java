package es.jbp.kajtools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.util.List;

public class EnvironmentConfiguration {

  public static List<Environment> ENVIRONMENT_LIST;

  private EnvironmentConfiguration() {
  }

  public static void loadEnvironmentConfig() throws IOException {
    ENVIRONMENT_LIST = new ObjectMapper(new YAMLFactory())
        //.setPropertyNamingStrategy(PropertyNamingStrategy.KEBAB_CASE)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .readValue(EnvironmentConfiguration.class.getResourceAsStream("/environments.yml"),
            new TypeReference<List<Environment>>() {
            });
  }

}
