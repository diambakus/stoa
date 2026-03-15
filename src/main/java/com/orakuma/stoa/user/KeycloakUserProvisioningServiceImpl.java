package com.orakuma.stoa.user;

import com.orakuma.stoa.config.security.KeycloakAdminProperties;
import jakarta.ws.rs.core.Response;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class KeycloakUserProvisioningServiceImpl implements KeycloakUserProvisioningService {
    private final Keycloak keycloak;
    private final KeycloakAdminProperties adminProperties;
    private final UserMapper userMapper;

    @Transactional
    @Override
    public String createUser(UserInKeycloakDto userInKeycloakDto) {
        RealmResource realmResource = keycloak.realm(adminProperties.getRealm());
        UsersResource usersResource = realmResource.users();

        var userRepresentation = userMapper.toUserRepresentation(userInKeycloakDto);
        userRepresentation.setEnabled(true);
        userRepresentation.setEmailVerified(false);

        var response = usersResource.create(userRepresentation);
        String userId = extractId(response, "Failed to create user: HTTP");

        var credentialRepresentation = new CredentialRepresentation();
        credentialRepresentation.setType(CredentialRepresentation.PASSWORD);
        credentialRepresentation.setTemporary(false);
        credentialRepresentation.setValue(userInKeycloakDto.rawPassword());
        realmResource.users().get(userId).resetPassword(credentialRepresentation);

        var niasseClient = realmResource.clients().findByClientId(adminProperties.getClientId()).stream().findFirst().orElseThrow();

        String clientUuid = niasseClient.getId();
        var employeeRole = realmResource.clients().get(clientUuid).roles().get(adminProperties.getEmployeeRole()).toRepresentation();

        realmResource.users().get(userId).roles().clientLevel(clientUuid).add(List.of(employeeRole));

        return userId;
    }

    @Transactional
    @Override
    public boolean updateUserPassword(String userId, PasswordUpdateDto passwordUpdateDto) {

        RealmResource realmResource = keycloak.realm(adminProperties.getRealm());
        String username = getUsernameByUserId(userId, realmResource);
        var credentialRepresentation = new CredentialRepresentation();
        credentialRepresentation.setType(CredentialRepresentation.PASSWORD);
        if (verifyPassword(username, passwordUpdateDto.currentPassword())) {
            try {
                credentialRepresentation.setValue(passwordUpdateDto.newPassword());
                realmResource.users().get(userId).resetPassword(credentialRepresentation);
            } catch (Exception e) {
                log.error("Could not update password in Keycloak: {}", e.getMessage());
                throw new RuntimeException(e);
            }

        } else {
            log.info("Passwords do not match!");
            return false;
        }

        return true;
    }

    @Override
    public String updateUser(UserInKeycloakDto userInKeycloakDto) {
        return "";
    }

    @Override
    public String updateUserWithBelongingUnitAndRole() {
        return "";
    }

    private String extractId(Response response, String errorMessage) {
        if (response.getStatus() >= 400) {
            throw new IllegalStateException(errorMessage + response.getStatus());
        }
        return response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
    }

    private boolean verifyPassword(String username, String password) {
        try {
            Keycloak keycloak = KeycloakBuilder.builder()
                    .serverUrl(adminProperties.getAuthServerUrl())
                    .realm(adminProperties.getRealm())
                    .clientId(adminProperties.getAdmin().getClientId())
                    .clientSecret(adminProperties.getAdmin().getClientSecret())
                    .grantType(OAuth2Constants.PASSWORD)
                    .username(username)
                    .password(password)
                    .build();

            keycloak.tokenManager().getAccessToken();
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    private String getUsernameByUserId(String userId, RealmResource realmResource) {

        UserRepresentation user = realmResource
                .users()
                .get(userId)
                .toRepresentation();

        return user.getUsername();
    }
}
