package es.jbp.kajtools;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Environment {
    private String name;
    private String bootstrapServers;
    private String urlSchemaRegistry;
    private String userSchemaRegistry;
    private String passwordSchemaRegistry;
    private boolean autoRegisterSchemas;
    private String securityProtocol;
    private String saslMechanism;
    private String sslTruststoreLocation;
    private String sslTruststorePassword;
    private String saslJaasConfig;

    @Override public String toString() {
        return name;
    }
}
