package es.jbp.kajtools.batch;

import es.jbp.kajtools.ui.interfaces.InfoReportable;

import java.util.List;
import java.util.Map;

public interface BatchService {

    BatchContext start(Map<String, String> parameters, InfoReportable infoReportable);

    List<String> getParameterNames();
}
