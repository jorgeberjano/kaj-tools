package es.jbp.kajtools.schemaregistry;

import es.jbp.kajtools.Environment;
import es.jbp.kajtools.KajException;
import java.util.List;

public interface ISchemaRegistryService {

  enum SubjectType {
    key, value
  }

  default String getTopicKeySchema(String topic, Environment environment) throws KajException {
    return getLatestTopicSchema(topic, SubjectType.key, environment);
  }

  default String getTopicValueSchema(String topic, Environment environment) throws KajException {
    return getLatestTopicSchema(topic, SubjectType.value, environment);
  }

  default String getLatestTopicSchema(String topic, SubjectType type, Environment environment) throws KajException {
    return getLatestSubjectSchema(topic + "-" + type, environment);
  }

  List<String> getSubjectSchemaVersions(String schemaSubject, Environment environment) throws KajException;

  String getLatestSubjectSchema(String subjectName, Environment environment) throws KajException;

  String getSubjectSchemaVersion(String schemaSubject, String version, Environment environment) throws KajException;

  void deleteSubjectSchemaVersion(String schemaSubject, String version, Environment environment) throws KajException;

  void writeSubjectSchema(String schemaSubject, Environment environment, String jsonSchema) throws KajException;
}
