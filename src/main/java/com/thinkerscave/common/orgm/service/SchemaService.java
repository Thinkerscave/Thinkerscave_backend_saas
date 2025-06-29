package com.thinkerscave.common.orgm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class SchemaService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void createSchema(String schemaName) {
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);
    }

    public boolean schemaExists(String schemaName) {
        String sql = "SELECT schema_name FROM information_schema.schemata WHERE schema_name = ?";
        return jdbcTemplate.queryForList(sql, schemaName).size() > 0;
    }
}
