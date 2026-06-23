package com.thinkerscave.document.repository;

import com.thinkerscave.document.entity.Document;
import com.thinkerscave.document.enums.DocumentOwnerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByOwnerTypeAndOwnerIdAndActiveTrue(DocumentOwnerType ownerType, Long ownerId);
}
