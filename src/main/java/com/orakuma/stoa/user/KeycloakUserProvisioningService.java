package com.orakuma.stoa.user;

public interface KeycloakUserProvisioningService {
    String createUser(UserInKeycloakDto userInKeycloakDto);
    boolean updateUserPassword(String userId, PasswordUpdateDto passwordUpdateDto);
    String updateUser(UserInKeycloakDto userInKeycloakDto);
    String updateUserWithBelongingUnitAndRole();
}
