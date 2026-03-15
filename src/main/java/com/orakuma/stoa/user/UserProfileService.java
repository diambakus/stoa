package com.orakuma.stoa.user;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface UserProfileService {
    Optional<UserProfileDto> create(UserProfileDto userProfileDto);
    Optional<UserProfileDto> getUserProfile(String id);
    Optional<UserProfileDto> updateUserRole(String userId, String role);
    Optional<UserProfileDto> updateUserProfileDetails(String userId, Map<String, String> details);
    Optional<UserProfileDto> updateUserUnits(String userId, Set<Long> unitsIds);
}
