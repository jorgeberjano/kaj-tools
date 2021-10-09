package es.jbp.kajtools.i18n;

import java.lang.reflect.Method;
import java.util.ResourceBundle;
import org.springframework.stereotype.Service;

@Service
public class DynamicBuldleI18nService implements I18nService {
  private static Method cachedGetBundleMethod = null;
  private final String defaultPath = "messages";

  public String getMessage(String key) {
    return getMessageFromBundle(defaultPath, key);
  }

  public String getMessageFromBundle(String path, String key) {
    ResourceBundle bundle;
    try {
      Class<?> thisClass = this.getClass();
      if (cachedGetBundleMethod == null) {
        Class<?> dynamicBundleClass = thisClass.getClassLoader().loadClass("com.intellij.DynamicBundle");
        cachedGetBundleMethod = dynamicBundleClass.getMethod("getBundle", String.class, Class.class);
      }
      bundle = (ResourceBundle) cachedGetBundleMethod.invoke(null, path, thisClass);
    } catch (Exception e) {
      bundle = ResourceBundle.getBundle(path);
    }
    return bundle.getString(key);
  }
}
