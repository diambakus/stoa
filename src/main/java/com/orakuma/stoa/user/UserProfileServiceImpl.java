package com.orakuma.stoa.user;

import com.orakuma.stoa.utils.RepositoriesHandler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@AllArgsConstructor
@Slf4j
public class UserProfileServiceImpl implements UserProfileService {
    private final UserProfileRepository userProfileRepository;
    private final UserMapper userMapper;
    private final RepositoriesHandler repositoriesHandler;

    @Transactional
    @Override
    public Optional<UserProfileDto> create(UserProfileDto userProfileDto) {
        UserProfile userProfile = userMapper.toUserProfile(userProfileDto);
        userProfile.setCreated(LocalDateTime.now());
        UserProfile persistedUser = userProfileRepository.save(userProfile);
        return Optional.of(userMapper.toUserProfileDto(persistedUser));
    }

    @Transactional
    @Override
    public Optional<UserProfileDto> getUserProfile(String id) {
        return userProfileRepository.findById(id)
                .map(user -> {
                    user.setLastLogin(LocalDateTime.now());
                    return userMapper.toUserProfileDto(user);
                });
    }

    @Transactional
    @Override
    public Optional<UserProfileDto> updateUserRole(String userId, String role) {
        UserProfile userProfile = repositoriesHandler.getUserProfileById(userId);
        if (StringUtils.isNotBlank(role) && !StringUtils.equals(userProfile.getRole(), role)) {
            userProfile.setRole(role);
        }
        UserProfile persistedUser = userProfileRepository.save(userProfile);
        return Optional.of(userMapper.toUserProfileDto(persistedUser));
    }

    @Transactional
    @Override
    public Optional<UserProfileDto> updateUserProfileDetails(String userId, Map<String, String> details) {
        UserProfile userProfile = repositoriesHandler.getUserProfileById(userId);
        details.forEach((key, value) -> {
            if (StringUtils.isNotBlank(key) && !StringUtils.equals("displayName", key)) {
                userProfile.setDisplayName(value);
            }
        });
        UserProfile persistedUser = userProfileRepository.save(userProfile);
        return Optional.of(userMapper.toUserProfileDto(persistedUser));
    }

    @Transactional
    @Override
    public Optional<UserProfileDto> updateUserUnits(String userId, Set<Long> unitsIds) {
        UserProfile userProfile = repositoriesHandler.getUserProfileById(userId);
        userProfile.setUnitsId(unitsIds);
        UserProfile persistedUser = userProfileRepository.save(userProfile);
        return Optional.of(userMapper.toUserProfileDto(persistedUser));
    }
}
