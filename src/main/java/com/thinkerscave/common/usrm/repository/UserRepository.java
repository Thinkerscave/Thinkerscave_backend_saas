package com.thinkerscave.common.usrm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.thinkerscave.common.usrm.domain.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

	 Optional<User> findByUserName(String userName);

	Optional<User> findByEmail(String email);

	Optional<User> findByEmailIgnoreCase(String email);
	 
}
