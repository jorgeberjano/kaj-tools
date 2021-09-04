package es.jbp.kajtools.templates.symbols;

import es.jbp.expressions.Function;
import es.jbp.expressions.Value;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.util.CollectionUtils;

public abstract class AbstractFunction implements Function {

  protected final Map<String, Value> memory = new HashMap<>();

  protected Value getMemorizedValue(String key) {
    return memory.getOrDefault(key, null);
  }
  protected void memorizeValue(String key, Value randValue) {
    memory.put(key, randValue);
  }

  protected <T> T randomElement(List<T> list) {
    if (CollectionUtils.isEmpty(list)) {
      return null;
    }
    if (list.size() == 1) {
      return list.get(0);
    }

    int i = ThreadLocalRandom.current().nextInt(0, list.size());
    return list.get(i);
  }

  protected Long getParameterAsLong(List<Value> parameterList, int index, long defaultValue) {
    if (parameterList.size() > index) {
      return parameterList.get(index).toBigInteger().longValue();
    } else {
      return defaultValue;
    }
  }

  protected String getParameterAsString(List<Value> parameterList, int index, String defaultValue) {
    if (parameterList.size() > index) {
      return parameterList.get(index).toString();
    } else {
      return defaultValue;
    }
  }
   public boolean allowOmitParameters() {
    return true;
  }

}
