package com.thinkerscave.admission.repository;

import com.thinkerscave.admission.entity.CounselingNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CounselingNoteRepository extends JpaRepository<CounselingNote, Long> {

    List<CounselingNote> findByInquiryInquiryIdOrderByCreatedOnDesc(Long inquiryId);
}
