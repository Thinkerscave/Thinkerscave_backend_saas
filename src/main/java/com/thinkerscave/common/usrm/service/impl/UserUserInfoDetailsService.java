package com.thinkerscave.common.usrm.service.impl;

import com.thinkerscave.common.usrm.domain.User;
import com.thinkerscave.common.usrm.repository.UserRepository;
import com.thinkerscave.common.security.UserInfoUserDetails;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserUserInfoDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	@Override
	@Transactional
	public UserInfoUserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		Optional<User> userObj = userRepository.findByUserName(username);// fetch the user object from DB
		return userObj.map(UserInfoUserDetails::new) // convert the user to UserInfoDetails
				.orElseThrow(() -> new UsernameNotFoundException("User Not Found :" + username));

	}

}
