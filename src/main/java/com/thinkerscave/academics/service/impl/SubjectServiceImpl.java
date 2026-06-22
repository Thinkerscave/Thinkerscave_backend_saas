package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.dto.request.SubjectRequest;
import com.thinkerscave.academics.dto.response.SubjectResponse;
import com.thinkerscave.academics.entity.Subject;
import com.thinkerscave.academics.repository.SubjectRepository;
import com.thinkerscave.academics.service.SubjectService;
import com.thinkerscave.shared.exceptions.AlreadyExistsException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;

    @Override
    @Transactional
    public SubjectResponse create(SubjectRequest request) {
        if (subjectRepository.existsBySubjectCode(request.getSubjectCode())) {
            throw new AlreadyExistsException("Subject with code '" + request.getSubjectCode() + "' already exists");
        }
        Subject subject = new Subject();
        mapRequest(request, subject);
        subject.setActive(request.getActive() != null ? request.getActive() : true);
        return toResponse(subjectRepository.save(subject));
    }

    @Override
    @Transactional
    public SubjectResponse update(Long subjectId, SubjectRequest request) {
        Subject subject = findById(subjectId);
        if (subjectRepository.existsBySubjectCodeAndSubjectIdNot(request.getSubjectCode(), subjectId)) {
            throw new AlreadyExistsException("Subject code '" + request.getSubjectCode() + "' is already in use");
        }
        mapRequest(request, subject);
        if (request.getActive() != null) {
            subject.setActive(request.getActive());
        }
        return toResponse(subjectRepository.save(subject));
    }

    @Override
    public SubjectResponse getById(Long subjectId) {
        return toResponse(findById(subjectId));
    }

    @Override
    public List<SubjectResponse> getAll() {
        return subjectRepository.findByActiveOrderBySubjectNameAsc(true)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<SubjectResponse> search(String keyword) {
        return subjectRepository.findBySubjectNameContainingIgnoreCaseOrSubjectCodeContainingIgnoreCase(keyword, keyword)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deactivate(Long subjectId) {
        Subject subject = findById(subjectId);
        subject.setActive(false);
        subjectRepository.save(subject);
    }

    // ---- helpers ----

    private Subject findById(Long id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + id));
    }

    private void mapRequest(SubjectRequest request, Subject subject) {
        subject.setSubjectCode(request.getSubjectCode());
        subject.setSubjectName(request.getSubjectName());
        subject.setSubjectType(request.getSubjectType());
        subject.setRemarks(request.getRemarks());
    }

    private SubjectResponse toResponse(Subject subject) {
        return SubjectResponse.builder()
                .subjectId(subject.getSubjectId())
                .subjectCode(subject.getSubjectCode())
                .subjectName(subject.getSubjectName())
                .subjectType(subject.getSubjectType())
                .active(subject.getActive())
                .remarks(subject.getRemarks())
                .build();
    }
}
