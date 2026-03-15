package com.orakuma.stoa.config.security;

import lombok.Generated;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "keycloak")
@Setter
@Getter
@Generated
public class KeycloakAdminProperties {
    private String authServerUrl;
    private String realm;
    private Admin admin;
    private String clientId;
    private String employeeRole;

    @Getter
    @Setter
    public static class Admin {
        private String clientId;
        private String clientSecret;
    }
}
