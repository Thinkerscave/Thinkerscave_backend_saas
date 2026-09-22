package com.thinkerscave.platform.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkerscave.platform.dto.response.ProvisioningStepResponse;
import com.thinkerscave.platform.dto.response.ReleaseResponse;
import com.thinkerscave.platform.enums.OperationStatus;
import com.thinkerscave.platform.enums.ReleaseStatus;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.assertj.core.api.Assertions.assertThat;

class TenantReleaseApiContractTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void releaseResponseUsesFrontendVersionFields() {
        ReleaseResponse response = new ReleaseResponse(1L, "REL-2", "2.0.0", "2.0.0",
                "2", "7", ReleaseStatus.DRAFT, null, null, "admin", "notes");

        JsonNode json = mapper.valueToTree(response);

        assertThat(json.has("releaseId")).isTrue();
        assertThat(json.has("releaseVersion")).isTrue();
        assertThat(json.has("applicationVersion")).isTrue();
        assertThat(json.has("targetDatabaseVersion")).isTrue();
        assertThat(json.has("targetCatalogVersion")).isTrue();
        assertThat(json.get("status").asText()).isEqualTo("DRAFT");
    }

    @Test
    void provisioningStepUsesCodeLabelAndSequence() {
        ProvisioningStepResponse step = new ProvisioningStepResponse(
                "SCHEMA_AND_MIGRATION", "Schema And Migration", 2,
                OperationStatus.RUNNING, null, null, null);

        JsonNode json = mapper.valueToTree(step);

        assertThat(json.has("code")).isTrue();
        assertThat(json.has("label")).isTrue();
        assertThat(json.has("sequence")).isTrue();
        assertThat(json.has("stepKey")).isFalse();
        assertThat(json.has("displayOrder")).isFalse();
    }

    @Test
    void controllersExposeDocumentedRoots() {
        assertThat(root(MigrationOperationsController.class)).isEqualTo("/api/platform/migrations");
        assertThat(root(CatalogSyncController.class)).isEqualTo("/api/platform/catalog-sync");
        assertThat(root(TenantHealthController.class)).isEqualTo("/api/platform/tenant-health");
        assertThat(root(ReleaseOperationsController.class)).isEqualTo("/api/platform/releases");
    }

    private String root(Class<?> controller) {
        return controller.getAnnotation(RequestMapping.class).value()[0];
    }
}
