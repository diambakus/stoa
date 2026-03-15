package com.orakuma.stoa.user;

public record PasswordUpdateDto(
        String currentPassword,
        String newPassword
) {
}
