package com.orakuma.stoa.user;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping(value = "/stoa/v1/user-provisioning")
@AllArgsConstructor
@Slf4j
public class KeycloakUserController {
    private final KeycloakUserProvisioningService keycloakUserProvisioningService;
    private final UserProfileService userProfileService;
    private final UserMapper userMapper;

    @PostMapping(path = "/create")
    @ApiResponse(responseCode = "201", description = "")
    public ResponseEntity<Optional<UserProfileDto>> create(@RequestBody UserCreationDto userCreationDto) {
        log.info("Creating a new user");
        UserInKeycloakDto userInKeycloakDto = userMapper.toUserInKeycloakDto(userCreationDto);
        String userId = keycloakUserProvisioningService.createUser(userInKeycloakDto);
        if (StringUtils.isBlank(userId)) {
            log.info("User creation(in keycloak) failed!");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } else {
            UserProfileDto userProfileDto = userMapper.toUserProfileDto(userCreationDto).setId(userId);
            Optional<UserProfileDto> persistedUserProfileDto = userProfileService.create(userProfileDto);
            return new ResponseEntity<>(persistedUserProfileDto, HttpStatus.CREATED);
        }
    }

    @PatchMapping(path = "/update-password/{userId}")
    @ApiResponse(responseCode = "201", description = "Update user provisioning details")
    public ResponseEntity<Boolean> updateUserPassword(@PathVariable("userId") String userId, @RequestBody PasswordUpdateDto passwordUpdateDto) {
        boolean updatedPasswordResponse = keycloakUserProvisioningService.updateUserPassword(userId, passwordUpdateDto);
        if (updatedPasswordResponse) {
            return new ResponseEntity<>(true, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(false, HttpStatus.UNAUTHORIZED);
        }
    }
}