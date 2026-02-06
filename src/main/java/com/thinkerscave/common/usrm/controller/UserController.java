package com.thinkerscave.common.usrm.controller;

import com.thinkerscave.common.usrm.domain.RefreshToken;
import com.thinkerscave.common.usrm.domain.User;
import com.thinkerscave.common.usrm.dto.AuthRequest;
import com.thinkerscave.common.usrm.dto.JwtResponce;
import com.thinkerscave.common.usrm.dto.RefreshTokenRequest;
import com.thinkerscave.common.usrm.dto.UserResponseDTO;
import com.thinkerscave.common.usrm.service.LoginAttemptService;
import com.thinkerscave.common.usrm.service.RefreshTokenService;
import com.thinkerscave.common.usrm.service.UserService;
import com.thinkerscave.common.usrm.service.impl.JwtServiceImpl;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

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

	@Operation(summary = "Register a new user")
	@PostMapping("/register")
	public ResponseEntity<User> registerUser(@RequestBody User user) {
		User registeredUser = userService.registerUser(user);
		return ResponseEntity.ok(registeredUser);
	}

	/**
	 * Retrieves a list of all users.
	 *
	 * @return a ResponseEntity containing the list of users
	 */
	@Operation(summary = "List all registered users", parameters = {
			@io.swagger.v3.oas.annotations.Parameter(name = "X-Tenant-ID", description = "Tenant/Schema identifier", required = true, example = "public", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER)
	})
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Successfully retrieved list"),
			@ApiResponse(responseCode = "403", description = "Access denied (Admin only)"),
			@ApiResponse(responseCode = "401", description = "Unauthorized")
	})
	@GetMapping("/list")
	@PreAuthorize("hasAuthority('ROLE_ADMIN')")
	public ResponseEntity<List<UserResponseDTO>> listUsers() {
		List<UserResponseDTO> users = userService.listUsers();
		return ResponseEntity.ok(users);
	}

	/**
	 * Retrieves a user by their ID.
	 *
	 * @param id the ID of the user
	 * @return a ResponseEntity containing the user if found, or 404 if not
	 */
	@Operation(summary = "Get user by ID", parameters = {
			@io.swagger.v3.oas.annotations.Parameter(name = "X-Tenant-ID", description = "Tenant/Schema identifier", required = true, example = "public", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER)
	})
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "User found"),
			@ApiResponse(responseCode = "404", description = "User not found"),
			@ApiResponse(responseCode = "403", description = "Access denied (Admin only)"),
			@ApiResponse(responseCode = "401", description = "Unauthorized")
	})
	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('ROLE_ADMIN')")
	public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
		Optional<UserResponseDTO> userOpt = userService.getUserById(id);
		return userOpt.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
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
	public String generateKey() {
		// Generate a secure random key for HS256
		Key key = Jwts.SIG.HS512.key().build();
		// Encode the key as a Base64 string
		String base64EncodedKey = Encoders.BASE64.encode(key.getEncoded());
		return base64EncodedKey;
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
	 * @return a {@link JwtResponce} containing the access token and refresh token,
	 *         if authentication is successful
	 * @throws RuntimeException if the user is blocked or if authentication fails
	 */
	@Operation(summary = "Login with credentials and receive JWT + Refresh Token", parameters = {
			@io.swagger.v3.oas.annotations.Parameter(name = "X-Tenant-ID", description = "Tenant/Schema identifier (e.g., mumbai_school, delhi_school)", required = true, example = "mumbai_school", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER)
	})
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Authentication successful"),
			@ApiResponse(responseCode = "403", description = "User is blocked"),
			@ApiResponse(responseCode = "401", description = "Invalid credentials")
	})
	@PostMapping("/login")
	public JwtResponce authenticationAndGetToken(@RequestBody AuthRequest authRequest) {

		String username = authRequest.getUsername();

		// 1. Check if user is blocked
		if (loginAttemptService.isBlocked(username)) {
			throw new RuntimeException("User account is temporarily locked due to too many failed login attempts.");
		}

		try {
			// 2. Try authentication
			Authentication authentication = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(username, authRequest.getPassword()));

			// 3. If successful, record successful login
			loginAttemptService.loginSucceeded(username);

			// 4. Generate tokens
			RefreshToken refreshToken = refreshTokenService.createRefreshToken(username);
			return JwtResponce.builder()
					.accessToken(jwtServiceImpl.generateToken(username))
					.token(refreshToken.getToken())
					.build();

		} catch (AuthenticationException ex) {
			// 5. Record failed login attempt
			loginAttemptService.loginFailed(username);
			throw ex; // Let GlobalExceptionHandler handle the response
		}
	}

	/**
	 * Endpoint to refresh a JWT using a refresh token.
	 *
	 * @param refreshTokenRequest the request containing the refresh token
	 * @return a response containing the new JWT and the refresh token
	 */
	@Operation(summary = "Refresh the JWT using a valid refresh token", parameters = {
			@io.swagger.v3.oas.annotations.Parameter(name = "X-Tenant-ID", description = "Tenant/Schema identifier", required = false, example = "public", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER)
	})
	@PostMapping("/refreshToken")
	public JwtResponce refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest) {
		// Use the transactional service method to validate token and get initialized
		// user
		User user = refreshTokenService.validateAndGetUser(refreshTokenRequest.getToken());

		String accessToken = jwtServiceImpl.generateToken(user.getUserName());

		// Build the response with the new JWT and the refresh token
		return JwtResponce.builder()
				.accessToken(accessToken)
				.token(refreshTokenRequest.getToken())
				.build();
	}

	@Operation(summary = "Logout user and invalidate refresh token", parameters = {
			@io.swagger.v3.oas.annotations.Parameter(name = "X-Tenant-ID", description = "Tenant/Schema identifier", required = false, example = "public", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER)
	})
	@PostMapping("/logout")
	public ResponseEntity<String> logoutUser(@RequestBody RefreshTokenRequest refreshTokenRequest) {
		// Invalidate refresh token in DB
		refreshTokenService.deleteByToken(refreshTokenRequest.getToken());
		return ResponseEntity.ok("Logged out successfully");
	}

	@Operation(summary = "Get current logged-in user details")
	@GetMapping("/currentUserInfo")
	public ResponseEntity<UserResponseDTO> getCurrentUser(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			return ResponseEntity.status(401).build();
		}

		String username = authentication.getName();

		UserResponseDTO dto = userService.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

		return ResponseEntity.ok(dto);
	}

}
