package es.jbp.kajtools.templates.symbols;

import es.jbp.expressions.Value;
import java.util.HashMap;
import java.util.Map;

public abstract class MemoryFunction extends AbstractFunction {

  protected static final Map<String, Value> memory = new HashMap<>();

  protected static Value getMemorizedValue(String key) {
    return memory.getOrDefault(key, null);
  }
  protected static void memorizeValue(String key, Value value) {
    memory.put(key, value);
  }
}
