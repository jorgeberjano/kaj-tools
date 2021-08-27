package es.jbp.kajtools.templates.symbols;

import es.jbp.expressions.Value;
import es.jbp.kajtools.util.ResourceUtil;
import java.util.Arrays;
import java.util.List;
import org.springframework.util.CollectionUtils;

public class FileLineFunction extends AbstractFunction {

  @Override
  public Value evaluate(List<Value> parameterList) {
    if (CollectionUtils.isEmpty(parameterList)) {
      return null;
    }
    String fileName = getParameterAsString(parameterList, 0, null);
    if (fileName == null) {
      return null;
    }
    String content = ResourceUtil.readResourceString(fileName);
    List<String> lines =  Arrays.asList(content.split("[\r\n]+"));
    String result = randomElement(lines);
    return new Value(result);
  }

  @Override
  public int getParameterCount() {
    return 2;
  }
}

