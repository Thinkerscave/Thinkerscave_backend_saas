package com.thinkerscave.communication.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Exact-token helpers for {@code MessageThread.participantUserIdsCsv}.
 * Avoids substring false-positives (user {@code 1} matching {@code 21}, {@code 100}, etc.).
 */
public final class ParticipantCsv {

    private ParticipantCsv() {
    }

    public static String join(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return "";
        }
        Set<Long> unique = userIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return unique.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    public static boolean contains(String csv, Long userId) {
        if (csv == null || csv.isBlank() || userId == null) {
            return false;
        }
        String needle = "," + userId + ",";
        return ("," + csv.trim() + ",").contains(needle);
    }

    public static Set<Long> parse(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** Ensures {@code userId} is present in the CSV (idempotent). */
    public static String ensureContains(String csv, Long userId) {
        Set<Long> ids = new LinkedHashSet<>(parse(csv));
        if (userId != null) {
            ids.add(userId);
        }
        return join(ids);
    }
}
