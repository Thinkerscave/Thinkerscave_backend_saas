package com.thinkerscave.common.admission.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.thinkerscave.common.admission.domain.Inquiry;

@Repository
public interface InquiryRepository extends JpaRepository<Inquiry,Long> {
	
	Optional<Inquiry> findByInquiryIdAndIsDeletedFalse(Long inquiryId);

    List<Inquiry> findAllByIsDeletedFalseOrderByCreatedAtDesc();

    boolean existsByMobileNumberAndIsDeletedFalse(String mobileNumber);

}
