package com.thinkerscave.student.entity.converter;

import com.thinkerscave.student.enums.TransferStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class TransferStatusConverter implements AttributeConverter<TransferStatus, String> {

    @Override
    public String convertToDatabaseColumn(TransferStatus attribute) {
        return attribute != null ? attribute.name() : null;
    }

    @Override
    public TransferStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return TransferStatus.valueOf(dbData.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}