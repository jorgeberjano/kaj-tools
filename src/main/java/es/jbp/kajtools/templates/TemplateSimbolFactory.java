package es.jbp.kajtools.templates;

import es.jbp.expressions.FinalVariable;
import es.jbp.expressions.SymbolFactory;
import es.jbp.expressions.Function;
import es.jbp.expressions.Value;
import es.jbp.expressions.Variable;
import es.jbp.kajtools.templates.symbols.AnyFunction;
import es.jbp.kajtools.templates.symbols.DateTimeFunction;
import es.jbp.kajtools.templates.symbols.FragmentFunction;
import es.jbp.kajtools.templates.symbols.FileLineFunction;
import es.jbp.kajtools.templates.symbols.RandFunction;
import es.jbp.kajtools.templates.symbols.StrFunction;
import es.jbp.kajtools.templates.symbols.UuidFunction;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TemplateSimbolFactory implements SymbolFactory {

  private final Map<String, Variable> variables = new HashMap<>();
  private final Map<String, Function> functions = new HashMap<>();
  private static final String userName = System.getProperty("user.name");

  public TemplateSimbolFactory(Map<String, String> variables, TextTemplate textTemplate) {
    addVariable("true", new FinalVariable(true));
    addVariable("false", new FinalVariable(false));
    addVariable("username", new FinalVariable(userName));
    addVariable("uuid", () -> new Value(UUID.randomUUID().toString()));
    addFunction("uuid", new UuidFunction());
    addFunction("str", new StrFunction());
    addFunction("any", new AnyFunction());
    addFunction("rand", new RandFunction());
    addFunction("fileline", new FileLineFunction());
    addFunction("fragment", new FragmentFunction(textTemplate));
    addFunction("datetime", new DateTimeFunction());
    if (variables != null) {
      variables.forEach((k, v) -> this.variables.put(k.toLowerCase(), new FinalVariable(v)));
    }
  }

  private void addVariable(String name, Variable variable) {
    variables.put(name, variable);
  }

  private void addFunction(String name, Function function) {
    functions.put(name, function);
  }

  @Override
  public Variable createVariable(String nombre) {
    return variables.getOrDefault(nombre.toLowerCase(), null);
  }

  @Override
  public Function createFunction(String nombre) {
    return functions.getOrDefault(nombre.toLowerCase(), null);
  }

  @Override
  public Function createOperator(String nombre) {
    return null;
  }

}
