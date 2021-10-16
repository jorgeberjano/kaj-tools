package es.jbp.kajtools.script;

public interface ScriptNode {
  void execute(ExecutionContext context) throws ScriptExecutionException;
}
