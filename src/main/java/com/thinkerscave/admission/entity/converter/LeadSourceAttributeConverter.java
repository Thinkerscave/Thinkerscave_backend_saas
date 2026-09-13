package com.thinkerscave.admission.entity.converter;

import com.thinkerscave.admission.enums.LeadSource;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class LeadSourceAttributeConverter implements AttributeConverter<LeadSource, String> {

    @Override
    public String convertToDatabaseColumn(LeadSource attribute) {
        return attribute == null ? LeadSource.OTHER.name() : attribute.name();
    }

    @Override
    public LeadSource convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return LeadSource.OTHER;
        }
        String normalized = dbData.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        try {
            return LeadSource.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return LeadSource.OTHER;
        }
    }
}