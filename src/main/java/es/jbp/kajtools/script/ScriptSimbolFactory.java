package es.jbp.kajtools.script;

import es.jbp.expressions.Function;
import es.jbp.kajtools.script.symbols.PrintFunction;
import es.jbp.kajtools.templates.TemplateSimbolFactory;
import es.jbp.kajtools.script.symbols.SendFunction;
import es.jbp.kajtools.script.symbols.SleepFunction;
import lombok.Setter;

public class ScriptSimbolFactory extends TemplateSimbolFactory {

  @Setter
  private ExecutionContext context;

  @Override
  public Function getFunction(String name) {
    switch (name) {
      case "send":
        return new SendFunction(context);
      case "sleep":
        return new SleepFunction(context);
      case "print":
        return new PrintFunction(context);
    }
    return super.getFunction(name);
  }
}
