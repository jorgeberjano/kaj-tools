package es.jbp.kajtools.script.nodes;

import es.jbp.expressions.ExpressionException;
import es.jbp.kajtools.script.ExecutionContext;
import es.jbp.kajtools.script.exception.ScriptExecutionException;
import es.jbp.kajtools.util.TemplateExecutor;
import java.math.BigInteger;
import org.apache.commons.lang3.math.NumberUtils;

public class WhileNode extends IfNode {

  public WhileNode(String condition, int lineNumber) {
    super(condition, lineNumber);
  }

  @Override
  public void execute(ExecutionContext context) throws ScriptExecutionException {
    TemplateExecutor templateExecutor = context.getTemplateExecutor();

    while (meetsCondition(templateExecutor)) {
      if (context.getAbort().get()) {
        return;
      }
      super.execute(context);
    }
  }
}
