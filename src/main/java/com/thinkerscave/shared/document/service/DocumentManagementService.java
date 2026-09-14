package com.thinkerscave.shared.document.service;

import com.thinkerscave.shared.document.dto.StoreDocumentCommand;
import com.thinkerscave.shared.document.entity.ManagedDocument;
import com.thinkerscave.shared.document.enums.ManagedDocumentStatus;
import com.thinkerscave.shared.document.repository.ManagedDocumentRepository;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.shared.storage.LocalFileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DocumentManagementService {

    private final ManagedDocumentRepository repository;
    private final LocalFileStorageService fileStorageService;

    @Transactional
    public ManagedDocument store(StoreDocumentCommand command) {
        if (command == null || command.getContent() == null || command.getContent().length == 0) {
            throw new BadRequestException("Document content is required");
        }
        if (!StringUtils.hasText(command.getIdempotencyKey())) {
            throw new BadRequestException("Document idempotency key is required");
        }
        Optional<ManagedDocument> existing = repository.findByDocumentTypeAndOwnerTypeAndOwnerIdAndIdempotencyKey(
                command.getDocumentType(), command.getOwnerType(), command.getOwnerId(), command.getIdempotencyKey());
        if (existing.isPresent()) {
            return existing.get();
        }
        String hash = sha256(command.getContent());
        String path;
        try {
            path = fileStorageService.storeBytes(
                    command.getContent(),
                    command.getContentType(),
                    command.getDocumentType().name().toLowerCase(),
                    command.getFileName());
        } catch (Exception ex) {
            throw new BadRequestException("Failed to store document: " + ex.getMessage());
        }
        ManagedDocument doc = new ManagedDocument();
        doc.setDocumentType(command.getDocumentType());
        doc.setOwnerType(command.getOwnerType());
        doc.setOwnerId(command.getOwnerId());
        doc.setPeriodKey(command.getPeriodKey());
        doc.setStoragePath(path);
        doc.setFileName(command.getFileName());
        doc.setContentType(command.getContentType() != null ? command.getContentType() : "application/octet-stream");
        doc.setContentHash(hash);
        doc.setByteSize((long) command.getContent().length);
        doc.setIdempotencyKey(command.getIdempotencyKey());
        doc.setStatus(ManagedDocumentStatus.ACTIVE);
        return repository.save(doc);
    }

    @Transactional(readOnly = true)
    public ManagedDocument findById(Long documentId) {
        return repository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Managed document not found: " + documentId));
    }

    @Transactional(readOnly = true)
    public byte[] loadContent(Long documentId) {
        ManagedDocument doc = findById(documentId);
        try {
            Resource resource = fileStorageService.loadAsResource(doc.getStoragePath());
            return resource.getContentAsByteArray();
        } catch (Exception ex) {
            throw new BadRequestException("Failed to load document content");
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (Exception ex) {
            return HexFormat.of().formatHex(String.valueOf(bytes.length).getBytes(StandardCharsets.UTF_8));
        }
    }
}
