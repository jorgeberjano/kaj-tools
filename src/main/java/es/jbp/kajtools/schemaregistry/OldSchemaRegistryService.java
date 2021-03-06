package es.jbp.kajtools.schemaregistry;

import es.jbp.kajtools.Environment;
import es.jbp.kajtools.KajException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.ws.rs.NotFoundException;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

//@Service
public class OldSchemaRegistryService implements ISchemaRegistryService {

  private static final Object SUBJECT_PATH = "/subjects/";

  public String getTopicKeySchema(String topic, Environment environment)
      throws KajException {
    return getLatestTopicSchema(topic, SubjectType.key, environment);
  }

  public String getTopicValueSchema(String topic, Environment environment)
      throws KajException {
    return getLatestTopicSchema(topic, SubjectType.value, environment);
  }

  public String getLatestTopicSchema(String topic, SubjectType type, Environment environment)
      throws KajException {
    return getLatestSubjectSchema(topic + "-" + type, environment);
  }

  public String getLatestSubjectSchema(String subjectName, Environment environment)
      throws KajException {

    RestTemplate restTemplate = createRestTemplate(environment);
    StringBuilder urlBuilder = new StringBuilder(environment.getUrlSchemaRegistry());
    urlBuilder.append(SUBJECT_PATH);
    urlBuilder.append(subjectName);
    urlBuilder.append("/versions/latest");
    return getSchema(environment, restTemplate, urlBuilder);
  }

  public List<String> getSubjectSchemaVersions(String subjectName, Environment environment) {
    RestTemplate restTemplate = createRestTemplate(environment);
    StringBuilder urlBuilder = new StringBuilder(environment.getUrlSchemaRegistry());
    urlBuilder.append(SUBJECT_PATH);
    urlBuilder.append(subjectName);
    urlBuilder.append("/versions");

    HttpEntity request = new HttpEntity<String>(createHeaders(environment));
    ResponseEntity<String> response = restTemplate
        .exchange(urlBuilder.toString(), HttpMethod.GET, request, String.class);
    JSONArray respJson = new JSONArray(response.getBody());
    Iterable<Object> iterable = respJson::iterator;
    return StreamSupport.stream(iterable.spliterator(), false)
        .map(Object::toString)
        .collect(Collectors.toList());
  }

  public void deleteSubjectSchemaVersion(String subjectName, String version,
      Environment environment) throws KajException {
    RestTemplate restTemplate = createRestTemplate(environment);
    StringBuilder urlBuilder = new StringBuilder(environment.getUrlSchemaRegistry());
    urlBuilder.append(SUBJECT_PATH);
    urlBuilder.append(subjectName);
    urlBuilder.append("/versions/");
    urlBuilder.append(version);
    HttpEntity<Void> request = new HttpEntity<>(createHeaders(environment));

    try {
      ResponseEntity<Void> response = restTemplate
          .exchange(urlBuilder.toString(), HttpMethod.DELETE, request, Void.class);

      HttpStatus statusCode = response.getStatusCode();
      System.out.println("Borrado de esquema: " + statusCode.toString());

    } catch (RestClientException ex) {
      throw new KajException("No se ha podido borrar la version " + version
          + " del subject " + subjectName, ex);
    }
  }

  public String getSubjectSchemaVersion(String subjectName, String version,
      Environment environment) throws KajException {
    RestTemplate restTemplate = new RestTemplate();
    StringBuilder urlBuilder = new StringBuilder(environment.getUrlSchemaRegistry());
    urlBuilder.append(SUBJECT_PATH);
    urlBuilder.append(subjectName);
    urlBuilder.append("/versions/");
    urlBuilder.append(version);
    return getSchema(environment, restTemplate, urlBuilder);
  }

  private String getSchema(Environment environment, RestTemplate restTemplate, StringBuilder urlBuilder)
      throws KajException{
    try {
      HttpEntity<String> request = new HttpEntity<>(createHeaders(environment));
      ResponseEntity<String> response = restTemplate
          .exchange(urlBuilder.toString(), HttpMethod.GET, request, String.class);
      if (response.getBody() == null) {
        return "";
      }
      JSONObject respJson = new JSONObject(response.getBody());
      return respJson.get("schema").toString();
    } catch (RestClientException ex) {
      throw new KajException("No se ha podido obtener el esquema", ex);
    } catch (NotFoundException ex) {
      return null;
    }
  }

  private RestTemplate createRestTemplate(Environment environment) {
    RestTemplate restTemplate = new RestTemplate();
    return restTemplate;
  }

  private HttpHeaders createHeaders(Environment environment) {
    HttpHeaders headers = new HttpHeaders();
    if (!Objects.isNull(environment.getUserSchemaRegistry())) {
      headers.setBasicAuth(environment.getUserSchemaRegistry(),
          environment.getPasswordSchemaRegistry());
    }
    return headers;
  }

  @Data
  @AllArgsConstructor
  private static class PostSchemaBody {

    private String schema;
  }

  public void writeSubjectSchema(String subjectName, Environment environment, String jsonSchema) {
    RestTemplate restTemplate = new RestTemplate();
    StringBuilder urlBuilder = new StringBuilder(environment.getUrlSchemaRegistry());
    urlBuilder.append(SUBJECT_PATH);
    urlBuilder.append(subjectName);
    urlBuilder.append("/versions");

    HttpEntity request = new HttpEntity<PostSchemaBody>(new PostSchemaBody(jsonSchema), createHeaders(environment));
    restTemplate.exchange(urlBuilder.toString(), HttpMethod.POST, request, String.class);
  }
}
