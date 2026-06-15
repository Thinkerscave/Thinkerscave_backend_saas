package com.thinkerscave.student.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.io.IOException;

import com.thinkerscave.student.dto.StudentCreateRequest;
import com.thinkerscave.student.dto.StudentResponseDTO;
import com.thinkerscave.student.dto.StudentProfileResponse;
import com.thinkerscave.student.dto.MedicalDTO;
import com.thinkerscave.student.dto.TimelineDTO;

public interface StudentService {

	StudentResponseDTO createStudent(StudentCreateRequest request) throws IOException;
    
    StudentResponseDTO saveStudentWithDocuments(StudentCreateRequest request, MultipartFile photo, List<MultipartFile> documents, List<String> types) throws IOException;

	StudentResponseDTO updateStudent(Long studentId, StudentCreateRequest request);

	StudentResponseDTO getStudentById(Long studentId);
    
    StudentProfileResponse getProfile360(Long studentId);
    
    StudentResponseDTO updatePersonal(Long studentId, StudentCreateRequest request);
    
    StudentResponseDTO updateMedical(Long studentId, MedicalDTO request);
    
    List<TimelineDTO> getTimeline(Long studentId);

	Page<StudentResponseDTO> getAllStudents(Pageable pageable);
    
    List<StudentResponseDTO> getAllStudents();

	void deleteStudent(Long studentId);
    
    List<com.thinkerscave.student.dto.StudentDocumentDTO> getStudentDocuments(Long studentId);
    
    Resource downloadDocument(Long docId);

}
