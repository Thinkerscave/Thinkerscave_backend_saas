package com.thinkerscave.staff.service;

import com.thinkerscave.document.entity.Document;
import com.thinkerscave.document.enums.DocumentOwnerType;
import com.thinkerscave.document.repository.DocumentRepository;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.shared.storage.LocalFileStorageService;
import com.thinkerscave.staff.dto.response.DocumentResponse;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.enums.StaffActivityType;
import com.thinkerscave.staff.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaffDocumentService {

    public static final String PHOTO_TYPE = "PHOTO";
    static final long MAX_PHOTO_BYTES = 2L * 1024 * 1024;
    private static final Set<String> PHOTO_MIME_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    /** Generic document types. PHOTO is excluded: photos go through the photo endpoints only. */
    static final Set<String> DOCUMENT_TYPES = Set.of(
            "AADHAR", "PAN", "RESUME", "QUALIFICATION", "EXPERIENCE", "OTHER");

    private final DocumentRepository documentRepository;
    private final StaffRepository staffRepository;
    private final LocalFileStorageService fileStorage;
    private final StaffActivityService activityService;

    @Transactional
    public DocumentResponse upload(Long staffId, MultipartFile file, String documentType, String documentName) {
        requireStaff(staffId);
        String type = normalizeType(documentType);
        fileStorage.validate(file);
        Document doc = new Document();
        fillFile(doc, staffId, type, documentName, file);
        deleteStoredFileIfRolledBack(doc.getFilePath());
        DocumentResponse response = toResponse(documentRepository.save(doc));
        activityService.record(requireStaff(staffId), StaffActivityType.DOCUMENT_UPLOADED, "Documents",
                "Uploaded " + type + " document");
        return response;
    }

    /** Stores a new profile photo; the previous photo row is deactivated and the staff photo URL is updated. */
    @Transactional
    public DocumentResponse uploadPhoto(Long staffId, MultipartFile file) {
        Staff staff = requireStaff(staffId);
        validatePhoto(file);
        documentRepository.findByOwnerTypeAndOwnerIdAndActiveTrue(DocumentOwnerType.STAFF, staffId).stream()
                .filter(d -> PHOTO_TYPE.equals(d.getDocumentType()))
                .forEach(d -> d.setActive(false));
        Document doc = new Document();
        fillFile(doc, staffId, PHOTO_TYPE, "Profile photo", file);
        Document saved = documentRepository.save(doc);
        staff.setPhotoUrl(photoUrl(staffId));
        activityService.record(staff, StaffActivityType.PHOTO_UPDATED, "Photo", "Profile photo updated");
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public DownloadedDocument loadPhoto(Long staffId) {
        requireStaff(staffId);
        Document photo = activePhoto(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Photo not found for staff: " + staffId));
        return new DownloadedDocument(fileStorage.loadAsResource(photo.getFilePath()),
                photo.getFileName(), photo.getMimeType());
    }

    @Transactional
    public void removePhoto(Long staffId) {
        Staff staff = requireStaff(staffId);
        activePhoto(staffId).ifPresent(p -> {
            p.setActive(false);
            activityService.record(staff, StaffActivityType.PHOTO_REMOVED, "Photo", "Profile photo removed");
        });
        staff.setPhotoUrl(null);
    }

    static String photoUrl(Long staffId) {
        return "/api/v1/staff/" + staffId + "/photo";
    }

    private Optional<Document> activePhoto(Long staffId) {
        return documentRepository.findByOwnerTypeAndOwnerIdAndActiveTrue(DocumentOwnerType.STAFF, staffId).stream()
                .filter(d -> PHOTO_TYPE.equals(d.getDocumentType()))
                .findFirst();
    }

    private void validatePhoto(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Photo file is required");
        }
        if (file.getSize() > MAX_PHOTO_BYTES) {
            throw new BadRequestException("Photo must be 2 MB or smaller");
        }
        if (file.getContentType() == null || !PHOTO_MIME_TYPES.contains(file.getContentType())) {
            throw new BadRequestException("Photo must be a JPG, PNG or WEBP image");
        }
    }

    /** Replaces a document: the old row is deactivated (kept for history) and a new row is created. */
    @Transactional
    public DocumentResponse replace(Long staffId, Long documentId, MultipartFile file, String documentName) {
        requireStaff(staffId);
        Document old = requireDocument(staffId, documentId);
        fileStorage.validate(file);
        Document doc = new Document();
        fillFile(doc, staffId, old.getDocumentType(),
                StringUtils.hasText(documentName) ? documentName : old.getDocumentName(), file);
        deleteStoredFileIfRolledBack(doc.getFilePath());
        old.setActive(false);
        documentRepository.save(old);
        DocumentResponse response = toResponse(documentRepository.save(doc));
        activityService.record(requireStaff(staffId), StaffActivityType.DOCUMENT_REPLACED, "Documents",
                "Replaced " + old.getDocumentType() + " document");
        return response;
    }

    @Transactional
    public void deactivate(Long staffId, Long documentId) {
        Staff staff = requireStaff(staffId);
        Document doc = requireDocument(staffId, documentId);
        doc.setActive(false);
        documentRepository.save(doc);
        activityService.record(staff, StaffActivityType.DOCUMENT_REMOVED, "Documents",
                "Removed " + doc.getDocumentType() + " document");
        log.info("Staff {} document {} deactivated", staffId, documentId);
    }

    @Transactional(readOnly = true)
    public DownloadedDocument download(Long staffId, Long documentId) {
        requireStaff(staffId);
        Document doc = requireDocument(staffId, documentId);
        if (!Boolean.TRUE.equals(doc.getActive())) {
            throw new ResourceNotFoundException("Document not found: " + documentId);
        }
        Resource resource = fileStorage.loadAsResource(doc.getFilePath());
        return new DownloadedDocument(resource, doc.getFileName(),
                doc.getMimeType() != null ? doc.getMimeType() : "application/octet-stream");
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> list(Long staffId) {
        requireStaff(staffId);
        return documentRepository.findByOwnerTypeAndOwnerIdAndActiveTrue(DocumentOwnerType.STAFF, staffId)
                .stream()
                .filter(d -> !PHOTO_TYPE.equals(d.getDocumentType()))
                .map(this::toResponse)
                .toList();
    }

    public record DownloadedDocument(Resource resource, String fileName, String contentType) {}

    /** Removes the stored file if the surrounding transaction rolls back, so no orphan file remains. */
    private void deleteStoredFileIfRolledBack(String storedPath) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    fileStorage.deleteQuietly(storedPath);
                }
            }
        });
    }

    private void fillFile(Document doc, Long staffId, String type, String name, MultipartFile file) {
        try {
            String path = fileStorage.store(file, "staff_" + staffId);
            doc.setOwnerType(DocumentOwnerType.STAFF);
            doc.setOwnerId(staffId);
            doc.setDocumentType(type);
            doc.setDocumentName(StringUtils.hasText(name) ? name.trim() : file.getOriginalFilename());
            doc.setFileName(fileStorage.sanitizeFilename(file.getOriginalFilename()));
            doc.setFilePath(path);
            doc.setFileExtension(extensionOf(doc.getFileName()));
            doc.setFileSize(file.getSize());
            doc.setMimeType(file.getContentType());
            doc.setActive(true);
        } catch (IOException ex) {
            throw new BadRequestException("Failed to store document: " + ex.getMessage());
        }
    }

    private String normalizeType(String documentType) {
        if (!StringUtils.hasText(documentType)) {
            throw new BadRequestException("Document type is required");
        }
        String type = documentType.trim().toUpperCase();
        if (PHOTO_TYPE.equals(type)) {
            throw new BadRequestException("Use the photo upload for profile photos");
        }
        if (!DOCUMENT_TYPES.contains(type)) {
            throw new BadRequestException("Unsupported document type: " + documentType);
        }
        return type;
    }

    private Staff requireStaff(Long staffId) {
        return staffRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found with ID: " + staffId));
    }

    private Document requireDocument(Long staffId, Long documentId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
        // PHOTO rows are only reachable through the photo endpoints.
        if (doc.getOwnerType() != DocumentOwnerType.STAFF || !staffId.equals(doc.getOwnerId())
                || PHOTO_TYPE.equals(doc.getDocumentType())) {
            throw new ResourceNotFoundException("Document not found: " + documentId);
        }
        return doc;
    }

    private static String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 && dot < fileName.length() - 1 ? fileName.substring(dot + 1).toLowerCase() : null;
    }

    private DocumentResponse toResponse(Document d) {
        return DocumentResponse.builder()
                .documentId(d.getDocumentId())
                .ownerType(d.getOwnerType().name())
                .ownerId(d.getOwnerId())
                .documentType(d.getDocumentType())
                .documentName(d.getDocumentName())
                .fileName(d.getFileName())
                .filePath(null)
                .fileExtension(d.getFileExtension())
                .fileSize(d.getFileSize())
                .mimeType(d.getMimeType())
                .active(d.getActive())
                .build();
    }
}
