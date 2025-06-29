package com.thinkerscave.common.usrm.service.impl;

import com.thinkerscave.common.usrm.domain.User;
import com.thinkerscave.common.usrm.repository.UserRepository;
import com.thinkerscave.common.usrm.security.UserInfoUserDetails;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserUserInfoDetailsService implements UserDetailsService {
@Autowired
private UserRepository userRepository;
	@Override
	public UserInfoUserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		// TODO Auto-generated method stub
		Optional<User> userObj=userRepository.findByUserName(username);//fetch the user object from DB
		return userObj.map(UserInfoUserDetails::new) //convert the user to UserInfoDetails
		.orElseThrow(()->new UsernameNotFoundException("User Not Found :"+username));
		
	}

}
