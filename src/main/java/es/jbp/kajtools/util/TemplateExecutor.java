package es.jbp.kajtools.util;

import com.github.jsontemplate.JsonTemplate;
import com.github.jsontemplate.valueproducer.IValueProducer;
import com.github.jsontemplate.valueproducer.Iso8601ValueProducer;
import com.google.common.collect.Lists;
import es.jbp.kajtools.util.jsontemplate.DateValueProducer;
import es.jbp.kajtools.util.jsontemplate.FileStringValueProducer;
import es.jbp.kajtools.util.jsontemplate.GlobalStringValueProducer;
import es.jbp.kajtools.util.jsontemplate.UuidValueProducer;
import java.util.List;
import java.util.Map;

public class TemplateExecutor {

  private List<IValueProducer> valueProducerList = Lists.newArrayList(
      new Iso8601ValueProducer(),
      new DateValueProducer(),
      new UuidValueProducer(),
      new FileStringValueProducer(),
      new GlobalStringValueProducer());

  private Map<String, Object> variables;

  public TemplateExecutor(Map<String, Object> variables) {
    this.variables = variables;
  }

  public String templateToJson(String template) {
    JsonTemplate jsonTemplate = new JsonTemplate(template).withVars(variables);
    valueProducerList.stream().forEach(p -> jsonTemplate.withValueProducer(p));
    return jsonTemplate.prettyString();
  }
}
