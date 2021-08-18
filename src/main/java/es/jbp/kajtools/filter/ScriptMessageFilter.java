package es.jbp.kajtools.filter;

import es.jbp.kajtools.KajException;
import es.jbp.kajtools.Key;
import es.jbp.kajtools.reflexion.Conversion;
import java.util.List;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.apache.commons.lang3.BooleanUtils;

public class ScriptMessageFilter implements MessageFilter {

  private final Invocable invocableFunction;


  public ScriptMessageFilter(String script) throws KajException {

    List<ScriptEngineFactory> factories = new ScriptEngineManager().getEngineFactories();
        
    ScriptEngine graalEngine = new ScriptEngineManager().getEngineByName("JavaScript");

    script = "function satisfy(jsonKey, jsonValue) {\n"
        + "var key = JSON.parse(jsonKey);\n"
        + "var value = JSON.parse(jsonValue);\n"
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
  public boolean satisfyCondition(String key, String value)  throws KajException{
    if (invocableFunction == null) {
      return true;
    }
    Object result = null;
    try {
      result = invocableFunction.invokeFunction("satisfy", key, value);
    } catch (ScriptException | NoSuchMethodException e) {
      throw new KajException("Error al invocar a la función de filtro", e);
    }
    return BooleanUtils.isTrue(Conversion.toBoolean(result));
  }
}
