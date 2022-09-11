package es.jbp.kajtools.templates;

import es.jbp.expressions.VariableWithValue;
import es.jbp.expressions.Function;
import es.jbp.expressions.SymbolFactory;
import es.jbp.expressions.Value;
import es.jbp.expressions.Variable;
import es.jbp.kajtools.templates.symbols.AnyFunction;
import es.jbp.kajtools.templates.symbols.DateTimeFunction;
import es.jbp.kajtools.templates.symbols.FileFunction;
import es.jbp.kajtools.templates.symbols.FileLineFunction;
import es.jbp.kajtools.templates.symbols.GetFunction;
import es.jbp.kajtools.templates.symbols.RandFunction;
import es.jbp.kajtools.templates.symbols.SetFunction;
import es.jbp.kajtools.templates.symbols.StrFunction;
import es.jbp.kajtools.templates.symbols.UuidFunction;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.Setter;

public class TemplateSimbolFactory implements SymbolFactory {

  @Setter
  private final Map<String, Variable> variables = new HashMap<>();
  private final Map<String, Function> functions = new HashMap<>();

  public TemplateSimbolFactory() {
  }

  public void declareSymbols(TextTemplate textTemplate) {
    // TODO: aclarar cuales se usan
    declareVariableValue("i", BigInteger.ZERO);
    declareVariableValue("count", BigInteger.ZERO);
    declareVariableValue("index", BigInteger.ZERO);
    declareVariableValue("counter", BigInteger.ZERO);
    declareVariable("true", new VariableWithValue(true));
    declareVariable("false", new VariableWithValue(false));
    declareVariable("username", new VariableWithValue(System.getProperty("user.name")));
    declareFunction("uuid", new UuidFunction());
    declareFunction("str", new StrFunction());
    declareFunction("any", new AnyFunction());
    declareFunction("rand", new RandFunction());
    declareFunction("fileline", new FileLineFunction()); // TODO: hacer que use tambien el textTemplate
    declareFunction("file", new FileFunction(textTemplate));
    declareFunction("fragment", new FileFunction(textTemplate));
    declareFunction("datetime", new DateTimeFunction());
    declareFunction("set", new SetFunction());
    declareFunction("get", new GetFunction());
  }

  public void declareVariable(String variableName) {
    variables.put(variableName.toLowerCase(), new VariableWithValue("")); // TODO: revisar esto
  }

  private void declareVariable(String name, Variable variable) {
    variables.put(name, variable);
  }

  private void declareFunction(String name, Function function) {
    functions.put(name, function);
  }

  @Override
  public Variable getVariable(String name) {
    return variables.getOrDefault(name.toLowerCase(), null);
  }

  @Override
  public Function getFunction(String name) {
    return functions.getOrDefault(name.toLowerCase(), null);
  }

  @Override
  public Function getOperator(String name) {
    return null;
  }

  public void declareVariables(Map<String, String> variableValueMap) {
    variableValueMap.forEach(this::declareVariableValue);
  }

  public void declareVariableValue(String variableName, String value) {
    variables.put(variableName.toLowerCase(), new VariableWithValue(value));
  }

  public void declareVariableValue(String variableName, BigInteger value) {
    variables.put(variableName.toLowerCase(), new VariableWithValue(value));
  }

  public String getVariableValue(String variableName) {
    return Optional.ofNullable(variables.get(variableName))
        .map(Variable::getValue)
        .map(Value::toString)
        .orElse(null);
  }

  public void assignVariableValue(String variableName, Value value) {
    if (variables.containsKey(variableName)) {
      variables.get(variableName).setValue(value);
    }

  }
}
