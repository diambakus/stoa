package com.orakuma.stoa.user;

public record UserInKeycloakDto(String id, String username, String email, String firstname, String lastname,
                                String rawPassword) {
}
