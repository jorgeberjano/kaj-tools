package es.jbp.kajtools.util.jsontemplate;

import com.github.jsontemplate.jsonbuild.JsonStringNode;
import com.github.jsontemplate.jsonbuild.supplier.ListParamSupplier;
import com.github.jsontemplate.valueproducer.StringValueProducer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class produces a {@link JsonStringNode} which generates a random string.
 */
public class GlobalStringValueProducer extends StringValueProducer {

  private Map<String, String> memory = new HashMap<>();

  /**
   * The type name used in the template, e.g. {anGSField: @gs}
   */
  public static final String TYPE_NAME = "gs";

  @Override
  public String getTypeName() {
    return TYPE_NAME;
  }

  /**
   * Produces a node which can generate a global random string.
   *
   * @return the produced json string node
   */
  @Override
  public JsonStringNode produce() {
    return new JsonStringNode(() -> super.produceString(getDefaultLength()));
  }

  /**
   * Produces a node which can generate a global random string referenced by a key.
   *
   * @param key the key of the global random string
   * @return the produced json string node
   */
  public JsonStringNode produce(String key) {
    return new JsonStringNode(() -> produceString(key, getDefaultLength()));
  }

  /**
   * Produces a node which select a global random string in a global list.
   *
   * @param keyList the enumerated string keys of the global random string
   * @return the produced json string node
   */
  @Override
  public JsonStringNode produce(List<String> keyList) {
    List<String> valueList = keyList.stream()
        .map(k -> produceString(k, getDefaultLength()))
        .collect(Collectors.toList());
    return new JsonStringNode(new ListParamSupplier<>(valueList));
  }

  @Override
  public JsonStringNode produce(Map<String, String> paramMap) {
    Map<String, String> copyParamMap = new HashMap<>(paramMap);

    String key = copyParamMap.remove("key");
    Integer length = pickIntegerParam(copyParamMap, "length");
    Integer min = pickIntegerParam(copyParamMap, "min");
    Integer max = pickIntegerParam(copyParamMap, "max");

    validateParamMap(copyParamMap);

    if (length != null) {
      shouldBePositive(length, "length");
      return new JsonStringNode(() -> produceString(key, length));

    } else if (min != null && max != null) {
      shouldBePositive(min, "min");
      shouldBePositive(max, "max");
      shouldBeInAscOrder(min, max, "min", "max");
      return new JsonStringNode(() -> produceString(key, randomIntInRange(min, max)));

    } else if (min != null) {
      // max == null
      shouldBePositive(min, "min");
      return new JsonStringNode(
          () -> produceString(key, randomIntInRange(min, getDefaultMax(min))));

    } else if (max != null) {
      // min == null
      shouldBePositive(max, "max");
      return new JsonStringNode(
          () -> produceString(key, randomIntInRange(getDefaultMin(max), max)));

    } else { // no expected parameters
      return produce();
    }
  }


  /**
   * Produces a random string
   *
   * @param key the key of the global random string
   * @return the random string for the key
   */
  protected String produceString(String key, int length) {
    String value = memory.get(key);
    if (value == null) {
      value = super.produceString(length);
      memory.put(key, value);
    }
    return value;
  }
}
