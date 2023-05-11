package es.jbp.kajtools.ksqldb;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KSqlScriptResult {
    private boolean ok;
    private String response;
}
