package com.orakuma.stoa.config.security;

import lombok.AllArgsConstructor;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AllArgsConstructor
public class KeycloakAdminConfig {

    @Bean
    public Keycloak keycloakAdmin(KeycloakAdminProperties keycloakAdminProperties) {
        return KeycloakBuilder.builder()
                .serverUrl(keycloakAdminProperties.getAuthServerUrl())
                .realm(keycloakAdminProperties.getRealm())
                .clientId(keycloakAdminProperties.getAdmin().getClientId())
                .clientSecret(keycloakAdminProperties.getAdmin().getClientSecret())
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .build();
    }
}