package com.thinkerscave.admission.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Maps legacy inquiry status strings still present in tenant data onto the
 * current {@link InquiryStatus} lifecycle. Flyway may be disabled in some
 * environments, so this converter keeps reads resilient.
 */
@Converter(autoApply = false)
public class InquiryStatusConverter implements AttributeConverter<InquiryStatus, String> {

    @Override
    public String convertToDatabaseColumn(InquiryStatus attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public InquiryStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        String normalized = normalize(dbData.trim());
        try {
            return InquiryStatus.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            // Unknown legacy value — treat as CONTACTED so list/work-queue screens keep working.
            return InquiryStatus.CONTACTED;
        }
    }

    static String normalize(String raw) {
        String key = raw.toUpperCase().replace(' ', '_').replace('-', '_');
        return switch (key) {
            case "FOLLOW_UP", "MEETING_SCHEDULED", "FOLLOW_UP_REQUIRED" -> InquiryStatus.CONTACTED.name();
            case "COUNSELING", "DOCUMENTS_PENDING", "READY_FOR_ADMISSION" -> InquiryStatus.INTERESTED.name();
            case "CONVERTED" -> InquiryStatus.APPLICATION_SUBMITTED.name();
            case "CLOSED" -> InquiryStatus.LOST.name();
            default -> key;
        };
    }
}
