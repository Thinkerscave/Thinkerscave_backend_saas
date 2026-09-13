package com.thinkerscave.admission.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Settings-driven required documents with conditional overrides for early entry
 * and previous schooling / TC.
 */
public final class RequiredDocumentsResolver {

    private RequiredDocumentsResolver() {
    }

    public static List<String> resolve(
            List<String> configured,
            String className,
            Boolean hasPreviousSchooling,
            String tcNumber,
            String previousSchoolName) {
        Set<String> types = new LinkedHashSet<>();
        if (configured != null) {
            for (String raw : configured) {
                String type = normalize(raw);
                if (!type.isEmpty() && !"OTHER".equals(type)) {
                    types.add(type);
                }
            }
        }

        boolean early = isEarlyEntryClass(className);
        boolean hasPrev = Boolean.TRUE.equals(hasPreviousSchooling)
                || hasText(tcNumber)
                || hasText(previousSchoolName);

        if (early) {
            types.remove("MARKSHEET");
            if (!hasPrev) {
                types.remove("TRANSFER_CERTIFICATE");
            }
        }
        if (hasPrev) {
            types.add("TRANSFER_CERTIFICATE");
        }
        return new ArrayList<>(types);
    }

    public static boolean isEarlyEntryClass(String className) {
        if (!hasText(className)) {
            return false;
        }
        String n = className.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
        if (n.matches(".*(nursery|pre kg|prekg|lkg|ukg|kg|prep|pre primary|play group|playgroup).*")) {
            return true;
        }
        return n.matches(".*(class|grade|std|standard)\\s*(i|1)(\\s|$).*")
                || n.equals("i")
                || n.equals("1");
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "_");
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
