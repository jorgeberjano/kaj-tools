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
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class SchemaRegistryService implements ISchemaRegistryService {

    private static final Object SUBJECT_PATH = "subjects/";

    private SSLContext sslContext;

//    @PostConstruct
//    public void init() {
//        // Se crea un TrustManager que no valida la cadena de certificados
//        TrustManager[] trustAllCerts = new TrustManager[]{
//                new X509TrustManager() {
//                    public X509Certificate[] getAcceptedIssuers() {
//                        return null;
//                    }
//
//                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
//                    }
//
//                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
//                    }
//                }
//        };
//        try {
//            sslContext = SSLContext.getInstance("SSL");
//            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
//
//            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
//        } catch (Exception ignored) {
//        }
//    }

    public List<String> getSubjects(Environment environment) throws KajException {
        String url = environment.getUrlSchemaRegistry()
                + SUBJECT_PATH;

        var response = sendGetRequest(url, environment);
        if (response.body() == null) {
            return Collections.emptyList();
        }
        Iterable<Object> iterable = new JSONArray(response.body());
        return StreamSupport.stream(iterable.spliterator(), false)
                .map(Object::toString)
                .collect(Collectors.toList());
    }

    public void deleteSubject(String subjectName, Environment environment) throws KajException {
        String url = environment.getUrlSchemaRegistry() + SUBJECT_PATH + subjectName;
        sendDeleteRequest(url, environment);
        sendDeleteRequest(url + "?permanent=true", environment);
    }

    public String getLatestSubjectSchema(String subjectName, Environment environment)
            throws KajException {

        String urlBuilder = environment.getUrlSchemaRegistry()
                + SUBJECT_PATH
                + subjectName
                + "/versions/latest";

        return getSchema(environment, urlBuilder);
    }

    public List<String> getSubjectSchemaVersions(String subjectName, Environment environment) throws KajException {
        String url = environment.getUrlSchemaRegistry()
                + SUBJECT_PATH
                + subjectName
                + "/versions";
        var response = sendGetRequest(url, environment);
        if (response.body() == null) {
            return Collections.emptyList();
        }
        JSONArray respJson = new JSONArray(response.body());
        Iterable<Object> iterable = respJson::iterator;
        return StreamSupport.stream(iterable.spliterator(), false)
                .map(Object::toString)
                .collect(Collectors.toList());
    }

    public void deleteSubjectSchemaVersion(String subjectName, String version,
                                           Environment environment) throws KajException {

        String url = environment.getUrlSchemaRegistry() + SUBJECT_PATH + subjectName + "/versions/" + version;
        sendDeleteRequest(url, environment);
        sendDeleteRequest(url + "?permanent=true", environment);
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

        HttpResponse<String> response = sendGetRequest(url, environment);
        if (response.body() == null) {
            return "";
        }
        JSONObject respJson = new JSONObject(response.body());
        return respJson.get("schema").toString();
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
                    .POST(HttpRequest.BodyPublishers.ofString(JsonUtils.instance.serialize(new PostSchemaBody(jsonSchema))))
                    .build();
            getHttpClient(environment).send(request, BodyHandlers.discarding());
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new KajException("No se ha podido grabar el esquema", e);
        }
    }


    private HttpResponse<String> sendGetRequest(String url, Environment environment) throws KajException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .GET()
                    .build();

            HttpResponse<String> response = getHttpClient(environment).send(request, BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new KajException("Respuesta de Schema Registry: " + response.body());
            }
            return response;
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new KajException("No se han podido obtener los subjects ", e);
        }
    }

    private HttpResponse<String> sendDeleteRequest(String url, Environment environment) throws KajException {
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
            return response;
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new KajException("No se han podido obtener los subjects ", e);
        }
    }
}
