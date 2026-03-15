package com.orakuma.stoa.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Set;

@Accessors(chain = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileDto {
    private String id;
    private String displayName;
    private Set<Long> unitsId;
    private String lastLogin;
    private String role;
}
