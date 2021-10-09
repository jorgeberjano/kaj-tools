package es.jbp.kajtools.i18n;

public interface I18nService {

  String getMessage(String key);

  String getMessageFromBundle(String path, String key);
}
