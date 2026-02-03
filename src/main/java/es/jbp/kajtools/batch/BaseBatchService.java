package es.jbp.kajtools.batch;

import es.jbp.kajtools.util.FileUtils;
import es.jbp.kajtools.util.JsonUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public abstract class BaseBatchService implements BatchService {

    protected void saveJsonToFile(Object object, String fileName, SimpleBatchContext context) {
        if (context.isStopping()) {
            return;
        }
        try {
            var json = JsonUtils.instance.serialize(object);
            saveStringToFile(json, fileName, context);
        } catch (IOException e) {
            context.error("No se ha podido guardar el archivo " + fileName, e);
        }
    }


    protected void saveStringToFile(String content, String fileName, SimpleBatchContext context) {
        if (context.isStopping()) {
            return;
        }
        try {
            var path = Path.of(fileName).getParent();
            Files.createDirectories(path);
            FileUtils.saveFile(fileName, content);
            context.trace("Saved " + fileName);
        } catch (IOException e) {
            context.error("No se ha podido guardar el archivo " + fileName, e);
        }
    }

    protected String extractRequestIdSuffix(String requestId) {
        return "[" +
                Optional.ofNullable(requestId)
                        .filter(r -> requestId.length() >= 6)
                        .map(r -> r.substring(requestId.length() - 6))
                        .map(r -> r.replace("/", "-"))
                        .map(r -> r.replace("*", "-"))
                        .orElse("------")
                + "]";
    }


    protected boolean missingParameters(SimpleBatchContext context) {
        return getParameterNames().stream()
                .filter(p -> StringUtils.isBlank(context.getParameter(p)))
                .peek(p -> context.error("El parámetro " + p + " debe tener un valor", null))
                .count() != 0;
    }

    protected void processInitially(SimpleBatchContext context) {
        context.setRunning(true);
        context.trace("Inicio de la ejecución");
    }

    protected void processFinally(SimpleBatchContext context) {
        context.setRunning(false);
        context.trace("Fin de la ejecución");
    }

    protected String ensurePath(SimpleBatchContext context, String parameterKey) throws IOException {
        var path = context.getParameter(parameterKey);
        if (!path.endsWith("\\") && !path.endsWith("/")) {
            path += File.separator;
            context.setParameter(parameterKey, path);
        }
        Files.createDirectories(Paths.get(path));
        return path;
    }
}


