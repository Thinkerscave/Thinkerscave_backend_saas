package com.thinkerscave.documentation.repository;

import com.thinkerscave.documentation.entity.DocCollectionBatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocCollectionBatchRepository extends JpaRepository<DocCollectionBatch, Long> {
    Page<DocCollectionBatch> findByOrganizationId(Long organizationId, Pageable pageable);
}
