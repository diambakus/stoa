package com.orakuma.stoa.user;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping(value = "/stoa/v1/user-profile")
@AllArgsConstructor
@Slf4j
@PreAuthorize("isAuthenticated()")
public class UserProfileController {
    private final UserProfileService userProfileService;

    @PostMapping(value = "/create")
    public ResponseEntity<Optional<UserProfileDto>> createUserProfile(@RequestBody UserProfileDto userProfileDto) {
        Optional<UserProfileDto> persistedUserProfileDto = userProfileService.create(userProfileDto);
        return new ResponseEntity<>(persistedUserProfileDto, HttpStatus.CREATED);
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<Optional<UserProfileDto>> getUserProfile(@PathVariable("id") String id) {
        Optional<UserProfileDto> persistedUserProfileDto = userProfileService.getUserProfile(id);
        return new ResponseEntity<>(persistedUserProfileDto, HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'ORGAN_ADMIN')")
    @PatchMapping
    public ResponseEntity<Optional<UserProfileDto>> updateUserRole(@RequestParam("userId") String userId, @RequestBody String role) {
        Optional<UserProfileDto> persistedUserProfileDto = userProfileService.updateUserRole(userId, role);
        return new ResponseEntity<>(persistedUserProfileDto, HttpStatus.OK);
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @PatchMapping("/{userId}")
    public ResponseEntity<Optional<UserProfileDto>> updateUserUnits(@PathVariable("userId") String userId, @RequestBody Set<Long> unitsIds) {
        Optional<UserProfileDto> persistedUserProfileDto = userProfileService.updateUserUnits(userId, unitsIds);
        return new ResponseEntity<>(persistedUserProfileDto, HttpStatus.OK);
    }


}