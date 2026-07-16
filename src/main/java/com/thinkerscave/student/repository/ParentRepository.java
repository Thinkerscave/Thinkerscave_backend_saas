package com.thinkerscave.student.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.thinkerscave.student.entity.Parent;

@Repository
public interface ParentRepository extends JpaRepository<Parent, Long> {

	Optional<Parent> findByMobileNumber(String mobileNumber);

	boolean existsByParentCode(String parentCode);

	/** Used to resolve the logged-in Parent's record for the Parent dashboard. */
	Optional<Parent> findByUser_Id(Long userId);
}
