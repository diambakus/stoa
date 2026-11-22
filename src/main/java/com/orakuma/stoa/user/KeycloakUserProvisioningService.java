package com.orakuma.stoa.user;

import java.util.Set;

public interface KeycloakUserProvisioningService {
    String createUser(UserInKeycloakDto userInKeycloakDto);
    boolean updateUserPassword(String userId, PasswordUpdateDto passwordUpdateDto);
    String updateUser(UserInKeycloakDto userInKeycloakDto);

    /** Replaces the user's units attribute in Keycloak; visible in their tokens after the next refresh. */
    void updateUserUnits(String userId, Set<Long> unitsId);
}
