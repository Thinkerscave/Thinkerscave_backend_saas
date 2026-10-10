package com.thinkerscave.documentation.service;

import com.thinkerscave.academics.entity.AcademicClass;
import com.thinkerscave.academics.repository.ClassRepository;
import com.thinkerscave.documentation.entity.*;
import com.thinkerscave.documentation.repository.*;
import com.thinkerscave.documentation.security.DocumentationAccessGuard;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.shared.storage.LocalFileStorageService;
import com.thinkerscave.student.entity.Student;
import com.thinkerscave.student.entity.StudentEnrollment;
import com.thinkerscave.student.repository.StudentEnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentationService {

    private final DocumentationAccessGuard guard;
    private final DocCollectionMasterRepository collectionMasters;
    private final StudyMaterialMasterRepository materialMasters;
    private final DocCollectionBatchRepository batches;
    private final DocCollectionItemRepository collectionItems;
    private final StudyMaterialHandoverRepository handovers;
    private final StudyMaterialHandoverItemRepository handoverItems;
    private final ClassRepository classes;
    private final StudentEnrollmentRepository enrollments;
    private final LocalFileStorageService files;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> classStudents(Long classId) {
        guard.requireView(DocumentationAccessGuard.DOCUMENTATION);
        if (classId == null) {
            throw new BadRequestException("Choose a class");
        }
        return enrollments.searchFrontDeskDirectory(classId, null, PageRequest.of(0, 500)).stream()
                .map(enrollment -> {
                    Student student = enrollment.getStudent();
                    Map<String, Object> row = new java.util.LinkedHashMap<>();
                    row.put("studentId", student.getStudentId());
                    row.put("studentName", studentName(student));
                    row.put("rollNumber", enrollment.getRollNumber());
                    return row;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<DocCollectionMaster> searchCollectionMasters(String status, String keyword, Pageable pageable) {
        guard.requireView(DocumentationAccessGuard.COLLECTION_MASTER);
        return collectionMasters.search(guard.organizationId(), blank(status), blank(keyword), pageable);
    }

    @Transactional(readOnly = true)
    public List<DocCollectionMaster> approvedCollectionMasters() {
        guard.requireView(DocumentationAccessGuard.COLLECTIONS);
        return collectionMasters.findByOrganizationIdAndStatus(guard.organizationId(), "APPROVED");
    }

    public DocCollectionMaster saveCollectionMaster(Long id, DocCollectionMaster body) {
        guard.requireManage(DocumentationAccessGuard.COLLECTION_MASTER);
        DocCollectionMaster row = id == null ? new DocCollectionMaster() : collectionMaster(id);
        if (id != null && "APPROVED".equals(row.getStatus())) {
            throw new BadRequestException("An approved master cannot be edited");
        }
        row.setOrganizationId(guard.organizationId());
        row.setMasterName(body.getMasterName());
        row.setCollectionType(body.getCollectionType());
        row.setDescription(body.getDescription());
        row.setClassIds(body.getClassIds());
        if (!StringUtils.hasText(row.getStatus()) || "REJECTED".equals(row.getStatus())) {
            row.setStatus("DRAFT");
            row.setRejectionReason(null);
        }
        return collectionMasters.save(row);
    }

    public DocCollectionMaster submitCollectionMaster(Long id) {
        guard.requireManage(DocumentationAccessGuard.COLLECTION_MASTER);
        DocCollectionMaster row = collectionMaster(id);
        row.setStatus("PENDING_APPROVAL");
        row.setRejectionReason(null);
        return collectionMasters.save(row);
    }

    public DocCollectionMaster approveCollectionMaster(Long id) {
        guard.requireApprove(DocumentationAccessGuard.COLLECTION_MASTER);
        DocCollectionMaster row = collectionMaster(id);
        row.setStatus("APPROVED");
        row.setRejectionReason(null);
        return collectionMasters.save(row);
    }

    public DocCollectionMaster rejectCollectionMaster(Long id, String reason) {
        guard.requireApprove(DocumentationAccessGuard.COLLECTION_MASTER);
        DocCollectionMaster row = collectionMaster(id);
        row.setStatus("REJECTED");
        row.setRejectionReason(reason);
        return collectionMasters.save(row);
    }

    @Transactional(readOnly = true)
    public Page<StudyMaterialMaster> searchMaterialMasters(String status, String keyword, Pageable pageable) {
        guard.requireView(DocumentationAccessGuard.MATERIAL_MASTER);
        return materialMasters.search(guard.organizationId(), blank(status), blank(keyword), pageable);
    }

    @Transactional(readOnly = true)
    public List<StudyMaterialMaster> approvedMaterialMasters() {
        guard.requireView(DocumentationAccessGuard.HANDOVERS);
        return materialMasters.findByOrganizationIdAndStatus(guard.organizationId(), "APPROVED");
    }

    public StudyMaterialMaster saveMaterialMaster(Long id, StudyMaterialMaster body) {
        guard.requireManage(DocumentationAccessGuard.MATERIAL_MASTER);
        StudyMaterialMaster row = id == null ? new StudyMaterialMaster() : materialMaster(id);
        if (id != null && "APPROVED".equals(row.getStatus())) {
            throw new BadRequestException("An approved master cannot be edited");
        }
        row.setOrganizationId(guard.organizationId());
        row.setMasterName(body.getMasterName());
        row.setMaterialType(body.getMaterialType());
        row.setDescription(body.getDescription());
        row.setClassIds(body.getClassIds());
        if (!StringUtils.hasText(row.getStatus()) || "REJECTED".equals(row.getStatus())) {
            row.setStatus("DRAFT");
            row.setRejectionReason(null);
        }
        return materialMasters.save(row);
    }

    public StudyMaterialMaster submitMaterialMaster(Long id) {
        guard.requireManage(DocumentationAccessGuard.MATERIAL_MASTER);
        StudyMaterialMaster row = materialMaster(id);
        row.setStatus("PENDING_APPROVAL");
        return materialMasters.save(row);
    }

    public StudyMaterialMaster approveMaterialMaster(Long id) {
        guard.requireApprove(DocumentationAccessGuard.MATERIAL_MASTER);
        StudyMaterialMaster row = materialMaster(id);
        row.setStatus("APPROVED");
        return materialMasters.save(row);
    }

    public StudyMaterialMaster rejectMaterialMaster(Long id, String reason) {
        guard.requireApprove(DocumentationAccessGuard.MATERIAL_MASTER);
        StudyMaterialMaster row = materialMaster(id);
        row.setStatus("REJECTED");
        row.setRejectionReason(reason);
        return materialMasters.save(row);
    }

    public Map<String, String> storeFile(MultipartFile file) throws IOException {
        guard.requireManage(DocumentationAccessGuard.COLLECTIONS);
        String path = files.store(file, "documentation");
        return Map.of("filePath", path, "fileName", file.getOriginalFilename() == null ? "document" : file.getOriginalFilename());
    }

    public DocCollectionBatch saveCollection(CollectionBatchRequest request) {
        guard.requireManage(DocumentationAccessGuard.COLLECTIONS);
        DocCollectionMaster master = collectionMaster(request.masterId());
        if (!"APPROVED".equals(master.getStatus())) {
            throw new BadRequestException("Choose an approved collection type");
        }
        AcademicClass academicClass = classes.findById(request.classId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        DocCollectionBatch batch = new DocCollectionBatch();
        batch.setOrganizationId(guard.organizationId());
        batch.setClassId(request.classId());
        batch.setClassName(academicClass.getName());
        batch.setMasterId(master.getId());
        batch = batches.save(batch);
        List<CollectionItemRequest> items = request.items() == null ? List.of() : request.items();
        if (Boolean.TRUE.equals(request.allStudents()) && items.isEmpty()) {
            items = enrollments.findByClassEntityClassIdAndActiveTrueOrderByStudentFirstNameAsc(request.classId()).stream()
                    .map(enrollment -> new CollectionItemRequest(enrollment.getStudent().getStudentId(), "", List.of()))
                    .toList();
        }
        for (CollectionItemRequest item : items) {
            StudentEnrollment enrollment = enrollments.findActiveWithClassByStudentId(item.studentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + item.studentId()));
            DocCollectionItem row = new DocCollectionItem();
            row.setBatchId(batch.getId());
            row.setStudentId(item.studentId());
            row.setStudentName(studentName(enrollment.getStudent()));
            row.setDocumentName(StringUtils.hasText(item.documentName()) ? item.documentName() : master.getCollectionType());
            row.setFilesJson(item.files() == null ? "[]" : item.files().stream()
                    .map(file -> "{\"fileName\":\"" + escape(file.fileName()) + "\",\"filePath\":\"" + escape(file.filePath()) + "\"}")
                    .collect(Collectors.joining(",", "[", "]")));
            collectionItems.save(row);
        }
        return batch;
    }

    @Transactional(readOnly = true)
    public Page<DocCollectionBatch> listCollections(Pageable pageable) {
        guard.requireView(DocumentationAccessGuard.COLLECTIONS);
        return batches.findByOrganizationId(guard.organizationId(), pageable);
    }

    @Transactional(readOnly = true)
    public List<DocCollectionItem> collectionItems(Long batchId) {
        guard.requireView(DocumentationAccessGuard.COLLECTIONS);
        batch(batchId);
        return collectionItems.findByBatchId(batchId);
    }

    public StudyMaterialHandover saveHandover(HandoverRequest request) {
        guard.requireManage(DocumentationAccessGuard.HANDOVERS);
        StudyMaterialMaster master = materialMaster(request.masterId());
        if (!"APPROVED".equals(master.getStatus())) {
            throw new BadRequestException("Choose an approved study material type");
        }
        AcademicClass academicClass = classes.findById(request.classId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        StudyMaterialHandover handover = new StudyMaterialHandover();
        handover.setOrganizationId(guard.organizationId());
        handover.setClassId(request.classId());
        handover.setClassName(academicClass.getName());
        handover.setMasterId(master.getId());
        handover = handovers.save(handover);
        List<HandoverItemRequest> items = request.items() == null ? new ArrayList<>() : request.items();
        if (Boolean.TRUE.equals(request.allStudents()) && items.isEmpty()) {
            items = enrollments.findByClassEntityClassIdAndActiveTrueOrderByStudentFirstNameAsc(request.classId()).stream()
                    .map(enrollment -> new HandoverItemRequest(enrollment.getStudent().getStudentId(), master.getDescription(), false, "NOT_HANDEDOVER"))
                    .toList();
        }
        for (HandoverItemRequest item : items) {
            StudentEnrollment enrollment = enrollments.findActiveWithClassByStudentId(item.studentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + item.studentId()));
            StudyMaterialHandoverItem row = new StudyMaterialHandoverItem();
            row.setHandoverId(handover.getId());
            row.setStudentId(item.studentId());
            row.setStudentName(studentName(enrollment.getStudent()));
            row.setDescription(item.description());
            row.setHandedOver(Boolean.TRUE.equals(item.handedOver()));
            row.setStatus(StringUtils.hasText(item.status()) ? item.status() : "NOT_HANDEDOVER");
            handoverItems.save(row);
        }
        return handover;
    }

    public StudyMaterialHandoverItem updateHandoverItem(Long itemId, HandoverItemRequest body) {
        guard.requireManage(DocumentationAccessGuard.HANDOVERS);
        StudyMaterialHandoverItem row = handoverItems.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Handover row not found"));
        handover(row.getHandoverId());
        row.setDescription(body.description());
        row.setHandedOver(Boolean.TRUE.equals(body.handedOver()));
        row.setStatus(body.status());
        return handoverItems.save(row);
    }

    @Transactional(readOnly = true)
    public Page<StudyMaterialHandover> listHandovers(Pageable pageable) {
        guard.requireView(DocumentationAccessGuard.HANDOVERS);
        return handovers.findByOrganizationId(guard.organizationId(), pageable);
    }

    @Transactional(readOnly = true)
    public List<StudyMaterialHandoverItem> handoverItems(Long handoverId, String status) {
        guard.requireView(DocumentationAccessGuard.HANDOVERS);
        handover(handoverId);
        return handoverItems.findByHandoverId(handoverId).stream()
                .filter(item -> !StringUtils.hasText(status) || status.equals(item.getStatus()))
                .toList();
    }

    private DocCollectionMaster collectionMaster(Long id) {
        return collectionMasters.findById(id)
                .filter(row -> row.getOrganizationId().equals(guard.organizationId()))
                .orElseThrow(() -> new ResourceNotFoundException("Collection master not found"));
    }

    private StudyMaterialMaster materialMaster(Long id) {
        return materialMasters.findById(id)
                .filter(row -> row.getOrganizationId().equals(guard.organizationId()))
                .orElseThrow(() -> new ResourceNotFoundException("Material master not found"));
    }

    private DocCollectionBatch batch(Long id) {
        return batches.findById(id)
                .filter(row -> row.getOrganizationId().equals(guard.organizationId()))
                .orElseThrow(() -> new ResourceNotFoundException("Collection batch not found"));
    }

    private StudyMaterialHandover handover(Long id) {
        return handovers.findById(id)
                .filter(row -> row.getOrganizationId().equals(guard.organizationId()))
                .orElseThrow(() -> new ResourceNotFoundException("Handover not found"));
    }

    private String studentName(Student student) {
        return (student.getFirstName() + " " + student.getLastName()).trim();
    }

    private String blank(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public record CollectionBatchRequest(Long classId, Long masterId, Boolean allStudents, List<CollectionItemRequest> items) {}
    public record CollectionItemRequest(Long studentId, String documentName, List<StoredFile> files) {}
    public record StoredFile(String fileName, String filePath) {}
    public record HandoverRequest(Long classId, Long masterId, Boolean allStudents, List<HandoverItemRequest> items) {}
    public record HandoverItemRequest(Long studentId, String description, Boolean handedOver, String status) {}
}
