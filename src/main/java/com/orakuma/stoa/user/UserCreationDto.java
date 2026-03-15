package com.orakuma.stoa.user;

import java.util.Set;

public record UserCreationDto(String username, String email, String firstname, String lastname,
                              String rawPassword, String role, Set<Long> unitsId, String displayName) {
}
