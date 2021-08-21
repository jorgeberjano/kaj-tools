package es.jbp.kajtools.util;

import java.util.Comparator;
import org.apache.commons.lang3.StringUtils;

public class JsonFirstComparator implements Comparator<String> {

  @Override
  public int compare(String s1, String s2) {
    if (StringUtils.isBlank(s1)) {
      return -1;
    }
    if (StringUtils.isBlank(s2)) {
      return 1;
    }
    boolean isS1Json = s1.endsWith(".json");
    boolean isS2Json = s2.endsWith(".json");
    if (isS1Json && !isS2Json) {
      return -1;
    }
    if (isS2Json && !isS1Json) {
      return 1;
    }
    return s1.compareTo(s2);
  }
}
