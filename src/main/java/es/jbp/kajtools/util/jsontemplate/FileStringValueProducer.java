package es.jbp.kajtools.util.jsontemplate;

import com.github.jsontemplate.jsonbuild.JsonStringNode;
import com.github.jsontemplate.valueproducer.AbstractValueProducer;
import es.jbp.kajtools.util.ResourceUtil;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

/**
 * This class produces a {@link JsonStringNode} which generates a string from a random line of a
 * text file.
 */
public class FileStringValueProducer extends AbstractValueProducer<JsonStringNode> {

  /**
   * The type name used in the template, e.g. {anDateField: @date}
   */
  public static final String TYPE_NAME = "file";

  @Override
  public String getTypeName() {
    return TYPE_NAME;
  }

  /**
   * Produces a node which can generate a string from a random line of a default.txt file
   *
   * @return the produced json string node
   */
  @Override
  public JsonStringNode produce() {
    return new JsonStringNode(() -> produceString("default"));
  }

  /**
   * Produces a node which can generate a string from one random line of a text file
   *
   * @param value the name of the file
   * @return the produced json string node
   */
  @Override
  public JsonStringNode produce(String value) {
    return new JsonStringNode(() -> produceString(value));
  }

  /**
   * Produces a string from random line of a file
   *
   * @return Date and time string
   */
  protected String produceString(String filename) {
    String content = ResourceUtil.readResourceString("jsontemplate/" + filename + ".txt");
    String[] lines = content.split("[\r\n]+");
    int i = ThreadLocalRandom.current().nextInt(0, lines.length);
    return lines[i];

  }
}
