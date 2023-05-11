package es.jbp.kajtools.ksqldb;

import es.jbp.kajtools.Environment;
import es.jbp.kajtools.KajException;
import es.jbp.kajtools.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
@RequiredArgsConstructor
public class KSqlDbService {

    private static final Object KSQL_PATH = "ksql/";

    private final SSLContext sslContext;

    public KSqlScriptResult executeScript(String script, Environment environment) throws KajException {

        if (environment.getUrlKSqlDbServer() == null) {
            throw new KajException("No se ha definido la URL del servidor KSQLDB");
        }

        String url = environment.getUrlKSqlDbServer() + KSQL_PATH;

        var command = KSqlRequest.builder().ksql(script).build();

        var handler = HttpResponse.BodyHandlers.ofString();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .POST(HttpRequest.BodyPublishers.ofString(JsonUtils.toJson(command)))
                    .build();
            var response = getHttpClient(environment).send(request, handler);
            return KSqlScriptResult.builder()
                    .ok(response.statusCode() == HttpStatus.OK.value())
                    .response(response.body())
                    .build();
            //    return JsonUtils.createFromJson(response.body(), KSqlErrorResponse.class);
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new KajException("No se ha podido ejecutar el script", e);
        }
    }

    private HttpClient getHttpClient(Environment environment) {
        return HttpClient.newBuilder()
                .sslContext(sslContext)
                .build();
    }
}
