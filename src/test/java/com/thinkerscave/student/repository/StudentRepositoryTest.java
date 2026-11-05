package com.thinkerscave.student.repository;

import com.thinkerscave.student.entity.Student;
import com.thinkerscave.student.enums.StudentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class StudentRepositoryTest {

    @Autowired private StudentRepository studentRepository;

    @Test
    void directoryQueryIncludesStudentsWithoutAdmissionNumbers() {
        Student student = new Student();
        student.setStudentCode("STU-001");
        student.setFirstName("Asha");
        student.setLastName("Rao");
        student.setStatus(StudentStatus.ACTIVE);
        studentRepository.saveAndFlush(student);

        var result = studentRepository.searchDirectory(
            null, null, null, null, null, null, null, null, null, PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        assertEquals("STU-001", result.getContent().get(0).getStudentCode());
        assertEquals(null, result.getContent().get(0).getAdmissionNumber());
    }
}
