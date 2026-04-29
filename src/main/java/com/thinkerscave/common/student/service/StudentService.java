package com.thinkerscave.common.student.service;

import com.thinkerscave.common.student.domain.Student;
import com.thinkerscave.common.student.dto.StudentRequestDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface StudentService {

    public com.thinkerscave.common.student.dto.StudentResponseDTO saveStudentWithDocuments(StudentRequestDTO dto,
            MultipartFile photo,
            List<MultipartFile> documents,
            List<String> types);

    public List<com.thinkerscave.common.student.dto.StudentResponseDTO> getAllStudents();

    public com.thinkerscave.common.student.dto.StudentResponseDTO getStudentById(Long id);

    public com.thinkerscave.common.student.dto.StudentResponseDTO updateStudent(Long id, StudentRequestDTO dto);

    public void deleteStudent(Long id);

    public List<com.thinkerscave.common.student.dto.StudentDocumentDTO> getStudentDocuments(Long studentId);

    public org.springframework.core.io.Resource downloadDocument(Long documentId);

}
