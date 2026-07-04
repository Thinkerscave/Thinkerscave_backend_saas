package com.thinkerscave.access.controller;

import com.thinkerscave.access.dto.request.ChangePasswordRequest;
import com.thinkerscave.access.dto.request.UpdateUserRequest;
import com.thinkerscave.access.dto.response.UserSummaryResponse;
import com.thinkerscave.access.service.ProfileService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "Self-service profile for the signed-in user")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/me")
    @Operation(summary = "Get the current user's profile")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> me() {
        return ResponseEntity.ok(ApiResponse.success("Profile fetched", profileService.getCurrentUser()));
    }

    @PutMapping("/me")
    @Operation(summary = "Update the current user's profile")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> updateMe(@Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Profile updated", profileService.updateCurrentUser(request)));
    }

    @PostMapping("/me/change-password")
    @Operation(summary = "Change the current user's password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        profileService.changeCurrentUserPassword(request);
        return ResponseEntity.ok(ApiResponse.noContent("Password changed successfully"));
    }
}
