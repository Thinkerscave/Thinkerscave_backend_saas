package com.thinkerscave.common.usrm.service;

import com.thinkerscave.common.usrm.domain.User;
import com.thinkerscave.common.usrm.dto.UserResponseDTO;

import java.util.List;
import java.util.Optional;

public interface UserService {

	User registerUser(User user);

	List<User> getUsers();

	List<UserResponseDTO> listUsers();

	Optional<UserResponseDTO> getUserById(Long id);

	Optional<User> findByEmail(String email);

	void updatePassword(User user, String password);
	

}
