package es.jbp.kajtools.util.jsontemplate;

import com.github.jsontemplate.jsonbuild.JsonStringNode;
import com.github.jsontemplate.valueproducer.AbstractValueProducer;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This class produces a {@link JsonStringNode} which generates current date and time string.
 */
public class DateValueProducer extends AbstractValueProducer<JsonStringNode> {

  private static final String DEFAULT_PATTERN = "yyyy-dd-MM'T'HH:MM'Z'";

  /**
   * The type name used in the template, e.g. {anDateField: @date}
   */
  public static final String TYPE_NAME = "date";

  @Override
  public String getTypeName() {
    return TYPE_NAME;
  }

  /**
   * Produces a node which can generate current date and time
   *
   * @return the produced json string node
   */
  @Override
  public JsonStringNode produce() {
    return new JsonStringNode(() -> produceDateTime(DEFAULT_PATTERN));
  }

  @Override
  public JsonStringNode produce(String value) {
    return new JsonStringNode(() -> produceDateTime(value));
  }

  /**
   * Produces the current date and time given a date-time format
   *
   * @return Date and time string
   */
  protected String produceDateTime(String pattern) {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
    return simpleDateFormat.format(new Date());

  }
}
