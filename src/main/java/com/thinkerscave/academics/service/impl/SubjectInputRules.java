package com.thinkerscave.academics.service.impl;

import com.thinkerscave.shared.exceptions.BusinessException;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.regex.Pattern;

/** Sanitization and search helpers shared by the subject catalog and curriculum mapping services. */
final class SubjectInputRules {

    static final int NAME_MAX = 150;
    static final int CODE_MAX = 50;
    static final int DESCRIPTION_MAX = 300;

    private static final Pattern HTML_TAG = Pattern.compile("<[^>]*>");
    private static final Pattern CONTROL_CHARS = Pattern.compile("\\p{Cntrl}");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern CODE_FORMAT = Pattern.compile("^[A-Z0-9][A-Z0-9 ._/&()-]*$");

    private SubjectInputRules() {
    }

    static String cleanText(String raw) {
        if (raw == null) return null;
        String value = HTML_TAG.matcher(raw).replaceAll(" ");
        value = CONTROL_CHARS.matcher(value).replaceAll(" ");
        value = WHITESPACE.matcher(value).replaceAll(" ").trim();
        return value.isEmpty() ? null : value;
    }

    static String requireName(String raw) {
        String name = cleanText(raw);
        if (name == null) throw new BusinessException("Subject name is mandatory");
        if (name.length() > NAME_MAX) throw new BusinessException("Subject name cannot exceed " + NAME_MAX + " characters");
        return name;
    }

    static String normalizeCode(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String code = cleanText(raw);
        if (code == null) throw new BusinessException("Subject code is invalid");
        code = code.toUpperCase(Locale.ROOT);
        if (code.length() > CODE_MAX) throw new BusinessException("Subject code cannot exceed " + CODE_MAX + " characters");
        if (!CODE_FORMAT.matcher(code).matches()) {
            throw new BusinessException("Subject code may contain letters, numbers, spaces and . _ / & ( ) -");
        }
        return code;
    }

    static String cleanDescription(String raw) {
        String description = cleanText(raw);
        if (description != null && description.length() > DESCRIPTION_MAX) {
            throw new BusinessException("Description cannot exceed " + DESCRIPTION_MAX + " characters");
        }
        return description;
    }

    /** Escapes LIKE wildcards with '!' and returns a lower-case contains pattern, or null when no search text is given. */
    static String containsPattern(String query) {
        if (!StringUtils.hasText(query)) return null;
        String needle = query.trim().toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + needle + "%";
    }
}
