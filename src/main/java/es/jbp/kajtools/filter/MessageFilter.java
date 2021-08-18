package es.jbp.kajtools.filter;

import es.jbp.kajtools.KajException;
import javax.script.ScriptException;

public interface MessageFilter {
  boolean satisfyCondition(String key, String value) throws KajException;
}
