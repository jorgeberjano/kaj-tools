package es.jbp.kajtools.script;

import org.apache.commons.lang3.math.NumberUtils;

public class SleepNode implements ScriptNode {
  private final String expression;

  public SleepNode(String expression) {
    this.expression = expression;
  }

  @Override
  public void execute(ExecutionContext context) throws ScriptExecutionException {
    try {
      Thread.sleep(calculateSleepTime());
    } catch (InterruptedException | NumberFormatException e) {
      throw new ScriptExecutionException("No se ha podido ejecutar el sleep", e);

    }
  }

  private long calculateSleepTime() {
    return NumberUtils.createLong(expression);
  }
}
