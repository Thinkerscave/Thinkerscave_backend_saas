package com.thinkerscave.common.usrm.service;

import com.thinkerscave.common.usrm.domain.User;
import com.thinkerscave.common.menum.domain.Role;
import com.thinkerscave.common.usrm.dto.UserCreationContext;
import com.thinkerscave.common.usrm.dto.UserResponseDTO;

import java.util.List;
import java.util.Optional;

public interface UserService {

	/**
	 * Creates a new user with generated credentials based on context.
	 */
	User createUser(UserCreationContext context, Role role);

	User registerUser(User user);

	List<User> getUsers();

	List<UserResponseDTO> listUsers();

	Optional<UserResponseDTO> getUserById(Long id);

	Optional<User> findByEmail(String email);

	void updatePassword(User user, String password);

	void updatePasswordAndInvalidateToken(User user, String newPassword);

	Optional<UserResponseDTO> findByUsername(String username);

}
