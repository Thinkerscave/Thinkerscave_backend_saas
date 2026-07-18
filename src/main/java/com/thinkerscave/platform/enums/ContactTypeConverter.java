package com.thinkerscave.platform.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

/**
 * Tolerant mapping for legacy / blank contact_type values.
 */
@Slf4j
@Converter(autoApply = true)
public class ContactTypeConverter implements AttributeConverter<ContactType, String> {

    @Override
    public String convertToDatabaseColumn(ContactType attribute) {
        return attribute == null ? ContactType.PRIMARY.name() : attribute.name();
    }

    @Override
    public ContactType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return ContactType.PRIMARY;
        }
        String normalized = dbData.trim().toUpperCase();
        try {
            return ContactType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            log.warn("Unknown contact_type '{}' — defaulting to PRIMARY", dbData);
            return ContactType.PRIMARY;
        }
    }
}
