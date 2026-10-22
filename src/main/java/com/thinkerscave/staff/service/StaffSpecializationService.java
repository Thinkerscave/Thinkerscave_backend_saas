package com.thinkerscave.staff.service;

import com.thinkerscave.academics.entity.Subject;
import com.thinkerscave.academics.repository.SubjectRepository;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.enums.StaffType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Sets the primary and secondary subjects of a staff member. */
@Service
@RequiredArgsConstructor
public class StaffSpecializationService {

    private final SubjectRepository subjectRepository;

    /** Non-teaching staff must have no subjects. Primary is required for teaching staff; secondary is optional. */
    public void assign(Staff staff, StaffType type, Long primaryId, Long secondaryId) {
        if (type != StaffType.TEACHING) {
            if (primaryId != null || secondaryId != null) {
                throw new BadRequestException("Subject specialization applies only to teaching staff");
            }
            staff.setPrimarySubject(null);
            staff.setSecondarySubject(null);
            return;
        }
        if (primaryId == null) {
            throw new BadRequestException("Primary subject is required for teaching staff");
        }
        List<Long> chosen = Stream.of(primaryId, secondaryId).filter(Objects::nonNull).toList();
        if (new HashSet<>(chosen).size() != chosen.size()) {
            throw new BadRequestException("A subject can be selected only once");
        }

        Map<Long, Subject> subjects = subjectRepository.findAllById(chosen).stream()
                .collect(Collectors.toMap(Subject::getSubjectId, Function.identity()));
        if (subjects.size() != chosen.size()) {
            throw new BadRequestException("One or more selected subjects were not found");
        }
        if (subjects.values().stream().anyMatch(s -> !Boolean.TRUE.equals(s.getActive()))) {
            throw new BadRequestException("Inactive subjects cannot be selected");
        }

        staff.setPrimarySubject(subjects.get(primaryId));
        staff.setSecondarySubject(secondaryId == null ? null : subjects.get(secondaryId));
    }
}
