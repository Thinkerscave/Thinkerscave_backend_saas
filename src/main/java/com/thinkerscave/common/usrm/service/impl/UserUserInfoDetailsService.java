package com.thinkerscave.common.usrm.service.impl;

import com.thinkerscave.common.usrm.domain.User;
import com.thinkerscave.common.usrm.repository.UserRepository;
import com.thinkerscave.common.security.UserInfoUserDetails;
import com.thinkerscave.common.menum.domain.Role;
import com.thinkerscave.common.menum.domain.RoleMenuPrivilegeMapping;
import com.thinkerscave.common.menum.repository.RoleMenuPrivilegeMappingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserUserInfoDetailsService implements UserDetailsService {

	private final UserRepository userRepository;
	private final RoleMenuPrivilegeMappingRepository roleMenuPrivilegeMappingRepository;

	@Override
	@Transactional(readOnly = true)
	public UserInfoUserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		// First try to find by username
		Optional<User> userObj = userRepository.findByUserName(username);

		// If not found, try to find by email
		if (userObj.isEmpty()) {
			userObj = userRepository.findByEmail(username);
		}

		User user = userObj.orElseThrow(() -> new UsernameNotFoundException("User Not Found :" + username));

		List<RoleMenuPrivilegeMapping> matrixMappings = new ArrayList<>();
		if (user.getRoles() != null && !user.getRoles().isEmpty()) {
			Role role = user.getRoles().iterator().next();
			matrixMappings = roleMenuPrivilegeMappingRepository.findByRoleId(role.getRoleId());
		}

		return new UserInfoUserDetails(user, matrixMappings);
	}

}
