package com.thinkerscave.common.student.service;

import com.thinkerscave.common.student.domain.Student;
import com.thinkerscave.common.student.dto.StudentRequestDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface StudentService {

    public Student saveStudentWithDocuments(StudentRequestDTO dto,
                                            MultipartFile photo,
                                            List<MultipartFile> documents,
                                            List<String> types);

}
