package es.jbp.kajtools.ksqldb;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class KSqlErrorResponse {

    @JsonProperty("@type")
    String type;
    @JsonProperty("error_code")
    Long errorCode;

    String message;

    String statementText;
}
