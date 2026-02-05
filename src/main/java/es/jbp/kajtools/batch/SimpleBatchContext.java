package es.jbp.kajtools.batch;

import es.jbp.kajtools.ui.InfoDocument;
import es.jbp.kajtools.ui.InfoMessage;
import es.jbp.kajtools.ui.interfaces.InfoReportable;
import es.jbp.kajtools.util.FileUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;


@RequiredArgsConstructor
public class SimpleBatchContext implements BatchContext {
    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    @Getter
    protected final Map<String, String> parameters;

    @Getter
    protected final InfoReportable infoReportable;

    protected AtomicBoolean stopDemanded = new AtomicBoolean();
    protected AtomicBoolean running = new AtomicBoolean();

    public String getParameter(String name) {
        return getParameter(name, null);
    }

    public String getParameter(String name, String defaultValue) {
        return Optional.ofNullable(parameters)
                .map(p -> p.get(name))
                .orElse(defaultValue);
    }

    public int getIntegerParameter(String mame, int defaultValue) {
        var value = getParameter(mame);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getBooleanParameter(String mame, boolean defaultValue) {
        var value = getParameter(mame);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Boolean.parseBoolean(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public void setParameter(String name, String value) {
        parameters.put(name, value);
    }

    public void setRunning(boolean b) {
        running.set(b);
        infoReportable.update();
    }

    public void stop() {
        stopDemanded.set(true);
    }

    public boolean isRunning() {
        return running.get();
    }

    public boolean isStopping() {
        return stopDemanded.get();
    }

    public void success(String text) {
        infoReportable.enqueueMessage(InfoReportable.buildSuccessfulMessage(text));
    }

    public void trace(String text) {
        String trace = LocalDateTime.now().format(formatter) + " - " + text;
        infoReportable.enqueueMessage(InfoReportable.buildTraceMessage(trace));
    }

    public void rawText(String text) {
        infoReportable.enqueueMessage(InfoReportable.buildTraceMessage(text));
    }

    public void error(String text, Throwable e) {
        infoReportable.enqueueMessage(InfoReportable.buildErrorMessage(text));
        if (e != null) {
            infoReportable.enqueueLink(InfoDocument.builder()
                    .type(InfoDocument.Type.INFO)
                    .title("Exception")
                    .left(new InfoMessage(extractExceptionInfo(e), InfoMessage.Type.TRACE))
                    .build());
        }
    }

    public void link(String linkText, String text, InfoDocument.Type type) {
        infoReportable.enqueueLink(InfoDocument.builder()
                .type(type)
                .title(linkText)
                .left(new InfoMessage(text, InfoMessage.Type.TRACE))
                .build());
    }

    private String extractExceptionInfo(Throwable e) {
        StringWriter out = new StringWriter();
        PrintWriter writer = new PrintWriter(out);
        e.printStackTrace(writer);
        writer.flush();
        return out.toString();
    }

    public List<Path> getFilesInFolderParameter(String parameterName, String mask) {
        var folder = parameters.get(parameterName);
        try {
            return FileUtils.findFilesInFolder(folder, 2, mask);
        } catch (Exception e) {
            infoReportable.enqueueMessage(InfoReportable.buildErrorMessage("No se ha podido listar la carpeta " + folder));
            infoReportable.enqueueException(e);
            return Collections.emptyList();
        }
    }
}
