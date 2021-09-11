package es.jbp.kajtools.kafka;

public enum RewindPolicy {
  LAST_MINUTE,
  LAST_HOUR,
  LAST_DAY,
  LAST_WEEK,
  LAST_MONTH,
  LAST_YEAR,
  BEGINNING
}
