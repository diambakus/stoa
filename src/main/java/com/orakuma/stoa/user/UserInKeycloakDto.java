package com.orakuma.stoa.user;

import java.util.Set;

/** @param unitsId units of the employee; written to the Keycloak user attribute (see KeycloakAdminProperties#unitsAttribute). */
public record UserInKeycloakDto(String id, String username, String email, String firstname, String lastname,
                                String rawPassword, Set<Long> unitsId) {
}
