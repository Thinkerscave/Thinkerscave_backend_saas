package com.thinkerscave.common.multitenancy;

import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "schema_meta_data", schema = "public")
@Data
public class SchemaMetaData {

    @Id
    @Tsid
    @Column(length = 50)
    private String id;

    @Column(name = "schema_name")
    private String schemaName;

    @Column(name = "url")
    private String url;

    @Column(name = "username")
    private String username;

    @Column(name = "password")
    private String password;

}