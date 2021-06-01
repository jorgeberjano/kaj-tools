package es.jbp.kajtools.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import es.jbp.kajtools.Environment;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SchemaRegistryService {

  private static final Object SUBJECT_PATH = "/subjects/";

  public String getTopicKeySchema(String topic, Environment environment)
      throws JsonProcessingException {
    return getLatestTopicSchema(topic, true, environment);
  }

  public String getTopicEventSchema(String topic, Environment environment)
      throws JsonProcessingException {
    return getLatestTopicSchema(topic, false, environment);
  }

  public String getLatestTopicSchema(String topic, boolean isKey, Environment environment)
      throws JsonProcessingException {
    return getLatestSubjectSchema(topic + (isKey ? "-key" : "-value"), environment);
  }

  public String getLatestSubjectSchema(String subjectName, Environment environment)
      throws JsonProcessingException {
    RestTemplate restTemplate = createRestTemplate(environment);
    StringBuilder urlBuilder = new StringBuilder(environment.getUrlSchemaRegistry());
    urlBuilder.append(SUBJECT_PATH);
    urlBuilder.append(subjectName);
    urlBuilder.append("/versions/latest");
    HttpEntity request = new HttpEntity<String>(createHeaders(environment));
    ResponseEntity<String> response = restTemplate
        .exchange(urlBuilder.toString(), HttpMethod.GET, request, String.class);
    JSONObject respJson = new JSONObject(response.getBody());
    return respJson.get("schema").toString();
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
    Iterable<Object> iterable = () -> respJson.iterator();
    return StreamSupport.stream(iterable.spliterator(), false)
        .map(Object::toString)
        .collect(Collectors.toList());
  }

  public void deleteSubjectSchemaVersion(String subjectName, String version,
      Environment environment) {
    RestTemplate restTemplate = createRestTemplate(environment);
    StringBuilder urlBuilder = new StringBuilder(environment.getUrlSchemaRegistry());
    urlBuilder.append(SUBJECT_PATH);
    urlBuilder.append(subjectName);
    urlBuilder.append("/versions/");
    urlBuilder.append(version);
    HttpEntity request = new HttpEntity<Void>(createHeaders(environment));
    restTemplate
        .exchange(urlBuilder.toString(), HttpMethod.DELETE, request, Void.class);
  }

  public String getSubjectSchemaVersion(String subjectName, String version,
      Environment environment) {
    RestTemplate restTemplate = new RestTemplate();
    StringBuilder urlBuilder = new StringBuilder(environment.getUrlSchemaRegistry());
    urlBuilder.append(SUBJECT_PATH);
    urlBuilder.append(subjectName);
    urlBuilder.append("/versions/");
    urlBuilder.append(version);
    HttpEntity request = new HttpEntity<String>(createHeaders(environment));
    ResponseEntity<String> response = restTemplate
        .exchange(urlBuilder.toString(), HttpMethod.GET, request, String.class);
    JSONObject respJson = new JSONObject(response.getBody());
    return respJson.get("schema").toString();
  }

  private RestTemplate createRestTemplate(Environment environment) {
    RestTemplate restTemplate = new RestTemplate();
//        if (!Objects.isNull(environment.getUserSchemaRegistry())) {
//            restTemplate.getInterceptors().add(
//                new BasicAuthenticationInterceptor(environment.getUserSchemaRegistry(),
//                    environment.getPasswordSchemaRegistry()));
//        }
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
}
