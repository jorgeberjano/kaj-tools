package es.jbp.kajtools.filter;

import es.jbp.kajtools.KajException;
import es.jbp.kajtools.kafka.RecordItem;

public interface MessageFilter {
  boolean satisfyCondition(RecordItem rec) throws KajException;
}
