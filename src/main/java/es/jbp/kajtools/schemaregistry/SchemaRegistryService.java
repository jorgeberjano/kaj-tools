package es.jbp.kajtools.schemaregistry;

import es.jbp.kajtools.Environment;
import es.jbp.kajtools.KajException;
import es.jbp.kajtools.util.JsonUtils;
import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.annotation.PostConstruct;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class SchemaRegistryService implements ISchemaRegistryService {

  private static final Object SUBJECT_PATH = "subjects/";

  private SSLContext sslContext;

  @PostConstruct
  public void init() {
    // Se crea un TrustManager que no valida la cadena de certificados
    TrustManager[] trustAllCerts = new TrustManager[]{
        new X509TrustManager() {
          public X509Certificate[] getAcceptedIssuers() {
            return null;
          }

          public void checkClientTrusted(X509Certificate[] certs, String authType) {
          }

          public void checkServerTrusted(X509Certificate[] certs, String authType) {
          }
        }
    };
    try {
      sslContext = SSLContext.getInstance("SSL");
      sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
    } catch (Exception e) {
    }
  }

  public String getLatestSubjectSchema(String subjectName, Environment environment)
      throws KajException {

    StringBuilder urlBuilder = new StringBuilder(environment.getUrlSchemaRegistry());
    urlBuilder.append(SUBJECT_PATH);
    urlBuilder.append(subjectName);
    urlBuilder.append("/versions/latest");

    return getSchema(environment, urlBuilder.toString());
  }

  public List<String> getSubjectSchemaVersions(String subjectName, Environment environment) throws KajException {
    String url = environment.getUrlSchemaRegistry()
        + SUBJECT_PATH
        + subjectName
        + "/versions";
    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(new URI(url))
          .GET()
          .build();

      HttpResponse<String> response = getHttpClient(environment).send(request, BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        throw new KajException("Respuesta de Schema Registry: " + response.body());
      }
      if (response.body() == null) {
        return Collections.emptyList();
      }
      JSONArray respJson = new JSONArray(response.body());
      Iterable<Object> iterable = respJson::iterator;
      return StreamSupport.stream(iterable.spliterator(), false)
          .map(Object::toString)
          .collect(Collectors.toList());
    } catch (URISyntaxException | IOException | InterruptedException e) {
      throw new KajException("No se ha podido obtener el esquema", e);
    }
  }

  public void deleteSubjectSchemaVersion(String subjectName, String version,
      Environment environment) throws KajException {

    String url = environment.getUrlSchemaRegistry() + SUBJECT_PATH + subjectName + "/versions/" + version;

    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(new URI(url))
          .DELETE()
          .build();
      getHttpClient(environment).send(request, BodyHandlers.discarding());

      HttpResponse<String> response = getHttpClient(environment).send(request, BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        throw new KajException("Respuesta de Schema Registry: " + response.body());
      }

    } catch (URISyntaxException | IOException | InterruptedException ex) {
      throw new KajException("No se ha podido borrar la version " + version + " de " + subjectName, ex);
    }
  }

  public String getSubjectSchemaVersion(String subjectName, String version, Environment environment)
      throws KajException {
    String url = environment.getUrlSchemaRegistry()
        + SUBJECT_PATH
        + subjectName
        + "/versions/"
        + version;
    return getSchema(environment, url);
  }

  private String getSchema(Environment environment, String url) throws KajException {

    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(new URI(url))
          .GET()
          .build();

      HttpResponse<String> response = getHttpClient(environment).send(request, BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        throw new KajException("Respuesta de Schema Registry: " + response.body());
      }
      if (response.body() == null) {
        return "";
      }
      JSONObject respJson = new JSONObject(response.body());
      return respJson.get("schema").toString();
    } catch (URISyntaxException | IOException | InterruptedException e) {
      throw new KajException("No se ha podido obtener el esquema", e);
    }
  }

  private HttpClient getHttpClient(Environment environment) {
    return HttpClient.newBuilder()
        .authenticator(getAuthenticator(environment))
        .sslContext(sslContext)
        .build();
  }

  private Authenticator getAuthenticator(final Environment environment) {
    return new Authenticator() {
      @Override
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(environment.getUserSchemaRegistry(),
            environment.getPasswordSchemaRegistry().toCharArray());
      }
    };
  }

  @Data
  @AllArgsConstructor
  private static class PostSchemaBody {

    private String schema;
  }

  public void writeSubjectSchema(String subjectName, Environment environment, String jsonSchema) throws KajException {

    String url = environment.getUrlSchemaRegistry() + SUBJECT_PATH + subjectName + "/versions";

    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(new URI(url))
          .POST(HttpRequest.BodyPublishers.ofString(JsonUtils.toJson(new PostSchemaBody(jsonSchema))))
          .build();
      getHttpClient(environment).send(request, BodyHandlers.discarding());
    } catch (URISyntaxException | IOException | InterruptedException e) {
      throw new KajException("No se ha podido grabar el esquema", e);
    }
  }
}
