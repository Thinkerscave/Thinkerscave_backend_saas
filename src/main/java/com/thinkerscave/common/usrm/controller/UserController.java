package com.thinkerscave.common.usrm.controller;

import com.thinkerscave.common.config.TenantContext;
import com.thinkerscave.common.service.TenantLookupService;
import com.thinkerscave.common.usrm.dto.*;
import com.thinkerscave.common.usrm.service.LoginAttemptService;
import com.thinkerscave.common.usrm.service.RefreshTokenService;
import com.thinkerscave.common.usrm.service.UserService;
import com.thinkerscave.common.usrm.service.impl.JwtServiceImpl;
import com.thinkerscave.common.commonModel.ApiResponse;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.security.Key;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Management", description = "APIs for managing Users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	private final JwtServiceImpl jwtServiceImpl;

	private final AuthenticationManager authenticationManager;

	private final RefreshTokenService refreshTokenService;

	private final LoginAttemptService loginAttemptService;

	private final TenantLookupService tenantLookupService;

	@Operation(summary = "Register a new user")
	@PostMapping("/register")
	public ResponseEntity<ApiResponse<UserResponseDTO>> registerUser(@Valid @RequestBody UserRequestDTO userRequestDTO) {
		log.info("Registering new user: {}", userRequestDTO.getUserName());
		UserResponseDTO savedUser = userService.registerUser(userRequestDTO);
		return ResponseEntity.status(201).body(ApiResponse.success("User registered successfully", savedUser));
	}

	/**
	 * Retrieves a list of all users.
	 *
	 * @return a ResponseEntity containing the list of users
	 */
	@Operation(summary = "List all registered users")
	@ApiResponses(value = {
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Successfully retrieved list"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied (Admin only)"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
	})
	@GetMapping("/list")
	@PreAuthorize("hasAuthority('ROLE_ADMIN')")
	public ResponseEntity<ApiResponse<List<UserResponseDTO>>> listUsers() {
		List<UserResponseDTO> users = userService.listUsers();
		return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
	}

	/**
	 * Retrieves a user by their ID.
	 *
	 * @param id the ID of the user
	 * @return a ResponseEntity containing the user if found, or 404 if not
	 */
	@Operation(summary = "Get user by ID")
	@ApiResponses(value = {
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User found"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied (Admin only)"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
	})
	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('ROLE_ADMIN')")
	public ResponseEntity<ApiResponse<UserResponseDTO>> getUserById(@PathVariable Long id) {
		return userService.getUserById(id)
				.map(dto -> ResponseEntity.ok(ApiResponse.success("User found", dto)))
				.orElseGet(() -> ResponseEntity.status(404).body(ApiResponse.error("User not found")));
	}

	/**
	 * Endpoint to generate a secure random Base64-encoded key for use with
	 * HS256/HS512 JWT signing algorithms.
	 * <p>
	 * This key can be copied and stored in the `application.properties` file with a
	 * property like:
	 * 
	 * <pre>
	 * jwt.secret = yourGeneratedBase64Key
	 * </pre>
	 *
	 * @return Base64-encoded secret key string
	 */
	@Operation(summary = "Generate Base64 JWT secret key (HS512)")
	@GetMapping("/generateKey")
	public ResponseEntity<ApiResponse<String>> generateKey() {
		Key key = Jwts.SIG.HS512.key().build();
		String base64EncodedKey = Encoders.BASE64.encode(key.getEncoded());
		return ResponseEntity.ok(ApiResponse.success("Key generated successfully", base64EncodedKey));
	}

	/**
	 * Authenticates the user based on provided credentials.
	 * <p>
	 * This method performs the following operations:
	 * <ul>
	 * <li>Checks if the user is temporarily blocked due to excessive failed login
	 * attempts using {@code LoginAttemptService}.</li>
	 * <li>Authenticates the user credentials using
	 * {@code AuthenticationManager}.</li>
	 * <li>On successful authentication, logs the success, creates and returns a JWT
	 * access token and a refresh token.</li>
	 * <li>On authentication failure, logs the failed attempt and returns an
	 * error.</li>
	 * </ul>
	 *
	 * @param authRequest the request object containing the username and password
	 * @return a {@link JwtResponse} containing the access token and refresh token,
	 *         if authentication is successful
	 * @throws RuntimeException if the user is blocked or if authentication fails
	 */
	@Operation(summary = "Login with credentials and receive JWT + Refresh Token", description = "Auto-detects tenant from username/email. X-Tenant-ID header is optional (for testing).")
	@ApiResponses(value = {
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Authentication successful"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "User is blocked"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials")
	})
	@PostMapping("/login")
	public ResponseEntity<ApiResponse<JwtResponse>> authenticationAndGetToken(@Valid @RequestBody AuthRequest authRequest) {

		String username = authRequest.getUsername();
		String tenantId = tenantLookupService.findTenantByEmailOrUsername(username);
		TenantContext.setTenant(tenantId);

		try {
			if (loginAttemptService.isBlocked(username)) {
				throw new org.springframework.security.authentication.LockedException(
						"User account is temporarily locked due to too many failed login attempts.");
			}

			Authentication authentication = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(username, authRequest.getPassword()));

			loginAttemptService.loginSucceeded(username);
			Optional<UserResponseDTO> userOpt = userService.findByUsername(username);
			com.thinkerscave.common.usrm.domain.RefreshToken refreshToken = refreshTokenService.createRefreshToken(username);

			JwtResponse response = JwtResponse.builder()
					.accessToken(jwtServiceImpl.generateToken(username))
					.token(refreshToken.getToken())
					.tenantId(tenantId)
					.tenantName(formatTenantName(tenantId))
					.user(userOpt.orElse(null))
					.build();

			return ResponseEntity.ok(ApiResponse.success("Login successful", response));

		} catch (AuthenticationException ex) {
			loginAttemptService.loginFailed(username);
			throw ex;
		} finally {
			TenantContext.clear();
		}
	}

	/**
	 * Helper method to format tenant ID into a human-readable name.
	 * Example: "mumbai_school" -> "Mumbai School"
	 */
	private String formatTenantName(String tenantId) {
		if (tenantId == null || tenantId.isEmpty() || "public".equals(tenantId)) {
			return "Public";
		}
		return java.util.Arrays.stream(tenantId.split("[_-]"))
				.map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
				.collect(java.util.stream.Collectors.joining(" "));
	}

	/**
	 * Endpoint to refresh a JWT using a refresh token.
	 *
	 * @param refreshTokenRequest the request containing the refresh token
	 * @return a response containing the new JWT and the refresh token
	 */
	@Operation(summary = "Refresh the JWT using a valid refresh token")
	@PostMapping("/refreshToken")
	public ResponseEntity<ApiResponse<JwtResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
		com.thinkerscave.common.usrm.domain.User user = refreshTokenService.validateAndGetUser(refreshTokenRequest.getToken());
		String accessToken = jwtServiceImpl.generateToken(user.getUserName());

		JwtResponse response = JwtResponse.builder()
				.accessToken(accessToken)
				.token(refreshTokenRequest.getToken())
				.build();
		return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
	}

	@Operation(summary = "Logout user and invalidate refresh token")
	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<Void>> logoutUser(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
		refreshTokenService.deleteByToken(refreshTokenRequest.getToken());
		return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
	}

	@Operation(summary = "Get current logged-in user details")
	@GetMapping("/currentUserInfo")
	public ResponseEntity<ApiResponse<UserResponseDTO>> getCurrentUser(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
		}
		String username = authentication.getName();
		UserResponseDTO dto = userService.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
		return ResponseEntity.ok(ApiResponse.success("User info retrieved", dto));
	}

	@Operation(summary = "Change password for authenticated user (first-time login)", description = "Must be called with a valid JWT. Changes password and clears the first-time-login flag.")
	@PatchMapping("/changePassword")
	public ResponseEntity<ApiResponse<Void>> changePassword(
			@Valid @RequestBody com.thinkerscave.common.usrm.dto.ChangePasswordRequest request,
			Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
		}
		if (!request.getNewPassword().equals(request.getConfirmPassword())) {
			return ResponseEntity.badRequest().body(ApiResponse.error("Passwords do not match."));
		}
		userService.changePasswordForCurrentUser(authentication.getName(), request.getNewPassword());
		return ResponseEntity.ok(ApiResponse.success("Password changed successfully.", null));
	}

}
