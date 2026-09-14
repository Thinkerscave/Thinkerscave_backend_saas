package com.thinkerscave.shared.document.repository;

import com.thinkerscave.shared.document.entity.ManagedDocument;
import com.thinkerscave.shared.document.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import com.thinkerscave.shared.document.enums.ManagedDocumentStatus;

public interface ManagedDocumentRepository extends JpaRepository<ManagedDocument, Long> {

    Optional<ManagedDocument> findByDocumentTypeAndOwnerTypeAndOwnerIdAndIdempotencyKey(
            DocumentType documentType, String ownerType, Long ownerId, String idempotencyKey);
    List<ManagedDocument> findByDocumentTypeAndOwnerTypeAndOwnerIdAndStatusOrderByCreatedOnAsc(
            DocumentType documentType, String ownerType, Long ownerId, ManagedDocumentStatus status);
}
