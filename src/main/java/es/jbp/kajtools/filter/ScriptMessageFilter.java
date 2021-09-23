package es.jbp.kajtools.filter;

import es.jbp.kajtools.KajException;
import es.jbp.kajtools.kafka.HeaderItem;
import es.jbp.kajtools.reflexion.Conversion;
import es.jbp.kajtools.kafka.RecordItem;
import es.jbp.kajtools.util.JsonUtils;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.apache.commons.lang3.BooleanUtils;

public class ScriptMessageFilter implements MessageFilter {

  private final Invocable invocableFunction;
  private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");


  public ScriptMessageFilter(String script) throws KajException {

    List<ScriptEngineFactory> factories = new ScriptEngineManager().getEngineFactories();
        
    ScriptEngine graalEngine = new ScriptEngineManager().getEngineByName("JavaScript");

    script = "function satisfy(jsonKey, jsonValue, jsonHeaders, partition, offset, date) {\n"
        + "var key = JSON.parse(jsonKey);\n"
        + "var value = JSON.parse(jsonValue);\n"
        + "var headers = JSON.parse(jsonHeaders);\n"
        + script
        + "\n}";
    try {
      graalEngine.eval(script);
    } catch (ScriptException e) {
      throw new KajException("No se ha podido evaluar el filtro", e);
    }

    invocableFunction = (Invocable) graalEngine;
  }

  @Override
  public boolean satisfyCondition(RecordItem rec)  throws KajException{
    if (invocableFunction == null) {
      return true;
    }
    Object result;
    try {
      Map<String, String> headersMap = rec.getHeaders().stream().collect(Collectors.toMap(HeaderItem::getKey, HeaderItem::getValue));
      String jsonHeaders = JsonUtils.toJson(headersMap);
      String datetime = rec.getDateTime().format(dateTimeFormatter);
      result = invocableFunction.invokeFunction("satisfy", rec.getKey(), rec.getValue(), jsonHeaders, rec.getPartition(),
          rec.getOffset(), datetime);
    } catch (Exception e) {
      throw new KajException("Error al invocar a la función de filtro", e);
    }
    return BooleanUtils.isTrue(Conversion.toBoolean(result));
  }
}
