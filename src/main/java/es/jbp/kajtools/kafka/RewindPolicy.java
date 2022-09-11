package es.jbp.kajtools.kafka;

public enum RewindPolicy {
  LAST_MINUTE,
  LAST_5_MINUTES,
  LAST_15_MINUTES,
  LAST_30_MINUTES,
  LAST_HOUR,
  LAST_DAY,
  LAST_WEEK,
  LAST_MONTH,
  LAST_YEAR,
  BEGINNING
}
