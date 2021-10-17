package es.jbp.kajtools.script.nodes;

import es.jbp.kajtools.script.ExecutionContext;
import es.jbp.kajtools.script.exception.ScriptExecutionException;
import lombok.Getter;
public abstract class ScriptNode {
  @Getter
  private final int line;

  public ScriptNode(int line) {
    this.line = line;
  }

  public abstract void execute(ExecutionContext context) throws ScriptExecutionException;
}
