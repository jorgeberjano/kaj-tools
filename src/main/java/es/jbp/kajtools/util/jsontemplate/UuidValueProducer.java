package es.jbp.kajtools.util.jsontemplate;

import com.github.jsontemplate.jsonbuild.JsonStringNode;
import com.github.jsontemplate.valueproducer.AbstractValueProducer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** This class produces a {@link JsonStringNode} which generates a UUID. */
public class UuidValueProducer extends AbstractValueProducer<JsonStringNode> {

  private final Map<String, String> memory = new HashMap<>();

  /** The type name used in the template, e.g. {anUuidField: @uuid} */
  public static final String TYPE_NAME = "uuid";

  @Override
  public String getTypeName() {
    return TYPE_NAME;
  }

  /**
   * Produces a node which can generate UUID
   *
   * @return the produced json string node
   */
  @Override
  public JsonStringNode produce() {
    return new JsonStringNode(this::produceUuid);
  }

  /**
   * Produces a {@link JsonStringNode} which can generate a UUID referenced by a key.
   *
   * @param key the key of the global random string
   * @return the produced json string node
   */
  public JsonStringNode produce(String key) {
    return new JsonStringNode(() -> produceUuid(key));
  }

  /**
   * Produces an random UUID
   *
   * @return the produced UUID
   */
  protected String produceUuid() {
    return UUID.randomUUID().toString();
  }

  /**
   * Produces an random UUID
   *
   * @param key the key of the UUID
   * @return the UUID for the key
   */
  protected String produceUuid(String key) {
    String value = memory.get(key);
    if (value == null) {
      value = produceUuid();
      memory.put(key, value);
    }
    return value;
  }


  @Override
  public JsonStringNode produce(Map<String, String> paramMap) {

    Map<String, String> copyParamMap = new HashMap<>(paramMap);
    String key = copyParamMap.remove("key");
    validateParamMap(copyParamMap);

    return new JsonStringNode(() -> produceUuid(key));
  }
}
