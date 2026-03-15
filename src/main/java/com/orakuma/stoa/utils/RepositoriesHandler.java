package com.orakuma.stoa.utils;

import com.orakuma.stoa.user.UserProfile;
import com.orakuma.stoa.user.UserProfileRepository;
import com.orakuma.stoa.user.exceptions.UserNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class RepositoriesHandler {
    private final UserProfileRepository profileRepository;

    public UserProfile getUserProfileById(String id) {
        return profileRepository.findById(id).orElseThrow(() -> {
            String errorMessage = String.format("User profile with id %s not found", id);
            return new UserNotFoundException(errorMessage);
        });
    }
}