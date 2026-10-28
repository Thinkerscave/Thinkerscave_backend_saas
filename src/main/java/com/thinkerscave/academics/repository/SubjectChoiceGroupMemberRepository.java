package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.SubjectChoiceGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface SubjectChoiceGroupMemberRepository extends JpaRepository<SubjectChoiceGroupMember, Long> {

    @Query("""
            SELECT m FROM SubjectChoiceGroupMember m
            JOIN FETCH m.subject s
            WHERE m.choiceGroup.subjectChoiceGroupId IN :groupIds
            ORDER BY LOWER(s.name), s.subjectId
            """)
    List<SubjectChoiceGroupMember> findDetailedByGroupIds(@Param("groupIds") Collection<Long> groupIds);

    List<SubjectChoiceGroupMember> findByChoiceGroup_SubjectChoiceGroupId(Long groupId);

    boolean existsBySubject_SubjectIdAndChoiceGroup_ActiveTrue(Long subjectId);
}
