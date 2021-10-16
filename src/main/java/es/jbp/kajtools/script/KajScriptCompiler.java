package es.jbp.kajtools.script;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public class KajScriptCompiler {

  public ScriptNode compile(String scriptCode) {
    String[] codeLines = StringUtils.split(scriptCode, "\\R");

    SequenceNode rootNode = new SequenceNode();

    processChildren(rootNode, codeLines, 0, 0);

//    Arrays.stream(codeLines)
//        .filter(StringUtils::isNotBlank)
//        .map(this::compileNode)
//        .filter(Objects::nonNull)
//        .forEach(rootNode::addChild);

    return rootNode;
  }

  private void processChildren(ScriptNode parentNode, String[] codeLines, int startIndex,
      int childrenIndentationLevel) {

    if (!(parentNode instanceof SequenceNode)) {
      // TODO: error
      System.err.println("Indentación incorrecta en linea " + startIndex);
      return;
    }

    int index = startIndex;
    ScriptNode lastNode = null;
    do {
      int currentIndentationLevel = identationLevel(codeLines[index]);
      if (currentIndentationLevel < childrenIndentationLevel) {
        return;
      }
      if (currentIndentationLevel > childrenIndentationLevel) {
        processChildren(lastNode, codeLines, index + 1, currentIndentationLevel);
      }

      lastNode = compileNode(codeLines[index]);

      ((SequenceNode) parentNode).addChild(lastNode);
    } while (++index < codeLines.length);
  }

  private int identationLevel(String line) {
    for (int i = 0; i < line.length(); i++) {
      if (line.charAt(i) != ' ') {
        return i;
      }
    }
    return 0;
  }

  private ScriptNode compileNode(String line) {
    String[] tokens = StringUtils.split(line, "\\s+");

    String command = ArrayUtils.get(tokens, 0, "").toLowerCase();
    switch (command) {
      case "set":
        return createSetVariableNode(tokens);
      case "for":
        return createForLoopNode(tokens);
      case "send":
        return createSendNode(tokens);
      case "sleep":
        return createSleepNode(tokens);
      default:
        reportError("Comando no reconocido: " + command);
        return null;
    }
  }

  private void reportError(String s) {
    System.err.println(s);
  }

  private ScriptNode createSetVariableNode(String[] tokens) {
    String variableName = ArrayUtils.get(tokens, 1, null);
    if (StringUtils.isBlank(variableName)) {
      reportError("Se esperaba el nombre de una variable");
      return null;
    }
    if (ArrayUtils.get(tokens, 2, null) != "=") {
      reportError("Se esperaba un signo =");
      return null;
    }
    String value = Arrays.stream(tokens).skip(3).collect(Collectors.joining(" "));
    return new SetVariableNode(variableName, value);
  }

  private ScriptNode createSleepNode(String[] tokens) {
    String expression = ArrayUtils.get(tokens, 1, "1000");
    return new SleepNode(expression);
  }

  private ScriptNode createSendNode(String[] tokens) {
    String topic = ArrayUtils.get(tokens, 1, null);
    if (StringUtils.isBlank(topic)) {
      reportError("Se esperaba el nombre de un topic");
      return null;
    }
    String key = ArrayUtils.get(tokens, 2, null);
    if (StringUtils.isBlank(key)) {
      reportError("Se esperaba una clave");
      return null;
    }
    String value = ArrayUtils.get(tokens, 3, null);
    String headers = ArrayUtils.get(tokens, 4, null);

    return new SendNode(topic, key, value, headers);
  }

  private ScriptNode createForLoopNode(String[] tokens) {
    String times = ArrayUtils.get(tokens, 1, null);
    return new LoopNode(times);
  }

}
