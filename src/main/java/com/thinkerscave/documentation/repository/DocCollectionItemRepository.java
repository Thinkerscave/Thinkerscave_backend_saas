package com.thinkerscave.documentation.repository;

import com.thinkerscave.documentation.entity.DocCollectionItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocCollectionItemRepository extends JpaRepository<DocCollectionItem, Long> {
    List<DocCollectionItem> findByBatchId(Long batchId);
}
