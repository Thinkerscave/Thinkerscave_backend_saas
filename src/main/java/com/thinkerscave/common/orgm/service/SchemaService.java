package com.thinkerscave.common.orgm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SchemaService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final List<String> REQUIRED_TABLES = List.of(
            "menu","organisation","users","owner_details","refresh_token","role","passwordresettoken"
    );

    public void createSchema(String schemaName) {
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);
    }

    public boolean schemaExists(String schemaName) {
        String sql = "SELECT schema_name FROM information_schema.schemata WHERE schema_name = ?";
        return jdbcTemplate.queryForList(sql, schemaName).size() > 0;
    }

    public List<String> getMissingTables(String schemaName) {
        if (!schemaExists(schemaName)) {
            throw new IllegalArgumentException("Schema does not exist: " + schemaName);
        }

        List<String> missingTables = new ArrayList<>();

        for (String table : REQUIRED_TABLES) {
            String sql = "SELECT COUNT(*) FROM information_schema.tables " +
                    "WHERE table_schema = ? AND table_name = ?";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, schemaName, table);
            if (count == null || count == 0) {
                missingTables.add(table);
            }
        }

        return missingTables;
    }

}
