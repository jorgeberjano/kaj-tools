package es.jbp.kajtools.script;

import java.util.ArrayList;
import java.util.List;

public class SequenceNode implements ScriptNode {

  private final List<ScriptNode> childNodes = new ArrayList<>();

  public void addChild(ScriptNode childNode) {
    this.childNodes.add(childNode);
  }

  @Override
  public void execute(ExecutionContext context) throws ScriptExecutionException {
    for (ScriptNode node : childNodes) {
      node.execute(context);
    }
  }
}
