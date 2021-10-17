package es.jbp.kajtools.script.nodes;

import es.jbp.kajtools.script.ExecutionContext;
import es.jbp.kajtools.script.exception.ScriptExecutionException;
import java.util.ArrayList;
import java.util.List;

public class SequenceNode extends ScriptNode {

  private final List<ScriptNode> childNodes = new ArrayList<>();

  public SequenceNode(int line) {
    super(line);
  }

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
