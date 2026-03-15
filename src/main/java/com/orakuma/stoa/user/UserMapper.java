package com.orakuma.stoa.user;

import org.keycloak.representations.idm.UserRepresentation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "firstName", source = "firstname")
    @Mapping(target = "lastName", source = "lastname")
    UserRepresentation toUserRepresentation(UserInKeycloakDto userInKeycloakDto);

    @Mapping(target = "rawPassword", ignore = true)
    UserInKeycloakDto toUserInKeycloakDto(UserRepresentation userRepresentation);

    @Mapping(target = "lastLogin", ignore = true)
    UserProfile toUserProfile(UserProfileDto userProfileDto);

    @Mapping(target = "lastLogin", expression = "java(toString(userProfile.getLastLogin()))")
    UserProfileDto toUserProfileDto(UserProfile userProfile);

    UserInKeycloakDto toUserInKeycloakDto(UserCreationDto userCreationDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lastLogin", ignore = true)
    UserProfileDto toUserProfileDto(UserCreationDto userCreationDto);

    default String toString(LocalDateTime localDateTime) {
        return localDateTime.toString();
    }
}
