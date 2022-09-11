package es.jbp.kajtools.script;

import es.jbp.kajtools.script.exception.ScriptCompilerException;
import es.jbp.kajtools.script.nodes.IfNode;
import es.jbp.kajtools.script.nodes.LoopNode;
import es.jbp.kajtools.script.nodes.ScriptNode;
import es.jbp.kajtools.script.nodes.SequenceNode;
import es.jbp.kajtools.script.nodes.SetVariableNode;
import es.jbp.kajtools.script.nodes.DoNode;
import es.jbp.kajtools.script.nodes.WhileNode;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public class ScriptCompiler {

  private final ScriptSimbolFactory symbolFactory;

  public ScriptCompiler(ScriptSimbolFactory symbolFactory) {
    this.symbolFactory = symbolFactory;
  }

  public ScriptNode compile(String scriptCode) throws ScriptCompilerException {

    String[] codeLines = scriptCode.split("\\R");

    List<String> codeLineList = Arrays.stream(codeLines)
        .filter(this::hasCommand)
        .collect(Collectors.toList());


    SequenceNode rootNode = new SequenceNode(0);

    processChildren(rootNode, codeLineList, 0, 0);

    return rootNode;
  }

  private boolean hasCommand(String lineCode) {
    lineCode = lineCode.trim();
    return StringUtils.isNotBlank(lineCode) && !lineCode.startsWith("#");
  }

  private int processChildren(ScriptNode parentNode, List<String> codeLines, int startLineNumber,
      int childrenIndentationLevel) throws ScriptCompilerException {

    if (!(parentNode instanceof SequenceNode)) {
      throw new ScriptCompilerException("Indentación incorrecta en la linea " + startLineNumber);
    }

    int lineNumber = startLineNumber;
    ScriptNode lastNode = null;
    while (lineNumber < codeLines.size()) {
      int currentIndentationLevel = indentationLevel(codeLines.get(lineNumber));
      if (currentIndentationLevel < childrenIndentationLevel) {
        break;
      }
      if (currentIndentationLevel > childrenIndentationLevel) {
        lineNumber = processChildren(lastNode, codeLines, lineNumber, currentIndentationLevel);
      } else {
        lastNode = compileNode(codeLines.get(lineNumber), lineNumber);
        ((SequenceNode) parentNode).addChild(lastNode);
        lineNumber++;
      }
    }
    return lineNumber;
  }

  private int indentationLevel(String line) {
    for (int i = 0; i < line.length(); i++) {
      if (line.charAt(i) != ' ') {
        return i;
      }
    }
    return 0;
  }

  private ScriptNode compileNode(String lineCode, int lineNumber) throws ScriptCompilerException {
    String[] tokens = lineCode.trim().split("\\s+", 2);

    String command = ArrayUtils.get(tokens, 0, "").toLowerCase();
    String rest = ArrayUtils.get(tokens, 1, "");
    switch (command) {
      case "set":
        return createSetVariableNode(rest, lineNumber);
      case "if":
        return createIfNode(rest, lineNumber);
      case "loop":
        return createLoopNode(rest, lineNumber);
      case "while":
        return createWhileNode(rest, lineNumber);
      case "do":
        return createDoNode(rest, lineNumber);
      default:
        throw new ScriptCompilerException("Comando no reconocido: " + command);
    }
  }

  private ScriptNode createSetVariableNode(String rest, int lineNumber) throws ScriptCompilerException {
    String[] tokens = rest.split("\\s+", 3);
    String variableName = ArrayUtils.get(tokens, 0, null);
    if (StringUtils.isBlank(variableName)) {
      throw new ScriptCompilerException("Se esperaba el nombre de una variable. Linea " + lineNumber);
    }
    if (!"=".equals(ArrayUtils.get(tokens, 1, null))) {
      throw new ScriptCompilerException("Se esperaba un signo =");
    }
    symbolFactory.declareVariable(variableName);

    String expression = ArrayUtils.get(tokens, 2, null);;
    if (StringUtils.isBlank(expression)) {
      throw new ScriptCompilerException("Se esperaba una expresión. Linea " + lineNumber);
    }
    return new SetVariableNode(variableName, expression, lineNumber);
  }

  private ScriptNode createDoNode(String rest, int lineNumber)  throws ScriptCompilerException {
    if (StringUtils.isBlank(rest)) {
      throw new ScriptCompilerException("Se esperaba una expresión. Linea " + lineNumber);
    }
    return new DoNode(rest, lineNumber);
  }

  private ScriptNode createLoopNode(String rest, int lineNumber) throws ScriptCompilerException {
    if (StringUtils.isBlank(rest)) {
      throw new ScriptCompilerException("Se esperaba una expresión. Linea " + lineNumber);
    }
    return new LoopNode(rest, lineNumber);
  }

  private ScriptNode createIfNode(String rest, int lineNumber) throws ScriptCompilerException {
    if (StringUtils.isBlank(rest)) {
      throw new ScriptCompilerException("Se esperaba una expresión. Linea " + lineNumber);
    }
    return new IfNode(rest, lineNumber);
  }

  private ScriptNode createWhileNode(String rest, int lineNumber) throws ScriptCompilerException {
    if (StringUtils.isBlank(rest)) {
      throw new ScriptCompilerException("Se esperaba una expresión. Linea " + lineNumber);
    }
    return new WhileNode(rest, lineNumber);
  }
}
