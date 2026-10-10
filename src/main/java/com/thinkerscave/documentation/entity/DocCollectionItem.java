package com.thinkerscave.documentation.entity;

import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Entity
@Getter
@Setter
@Table(name = "doc_collection_item")
public class DocCollectionItem extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long id;

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "student_name", nullable = false, length = 200)
    private String studentName;

    @Column(name = "document_name", nullable = false, length = 200)
    private String documentName;

    @Column(name = "files_json", columnDefinition = "TEXT")
    private String filesJson;
}
