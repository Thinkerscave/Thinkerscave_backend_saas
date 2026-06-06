package com.thinkerscave.common.admission.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.thinkerscave.common.admission.domain.CounselingNote;

@Repository
public interface CounselingNoteRepository extends JpaRepository<CounselingNote, Long> {

    List<CounselingNote> findByInquiry_InquiryIdOrderByCreatedDateDesc(Long inquiryId);
}
