package com.thinkerscave.common.common.sequence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Centralized sequence generator — gives monotonic, padded, prefixed numbers
 * per (organization, sequenceKey, context).
 *
 * <p>Examples:
 * <pre>{@code
 *   nextNumber(1L, "ADMISSION", "2026-27") → "ADM-2026-27-00001"
 *   nextNumber(1L, "INVOICE",   "2026-27") → "INV-2026-27-00001"
 * }</pre>
 *
 * <p>Uses {@code REQUIRES_NEW} + pessimistic locking so the counter update
 * commits independently of the calling business transaction (prevents gaps
 * from rolling back numbers, and avoids long lock holds).
 */
@Service
@RequiredArgsConstructor
public class SequenceGeneratorService {

    private final SequenceCounterRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String nextNumber(Long organizationId, String sequenceKey, String context) {
        SequenceCounter counter = repository
                .findByOrganizationIdAndSequenceKeyAndContext(organizationId, sequenceKey, context)
                .orElseGet(() -> repository.save(SequenceCounter.builder()
                        .organizationId(organizationId)
                        .sequenceKey(sequenceKey)
                        .context(context)
                        .prefix(defaultPrefix(sequenceKey))
                        .padding(5)
                        .lastNumber(0L)
                        .build()));

        long next = counter.getLastNumber() + 1;
        counter.setLastNumber(next);
        repository.save(counter);
        return format(counter, next);
    }

    public long nextRaw(Long organizationId, String sequenceKey, String context) {
        SequenceCounter counter = repository
                .findByOrganizationIdAndSequenceKeyAndContext(organizationId, sequenceKey, context)
                .orElseGet(() -> repository.save(SequenceCounter.builder()
                        .organizationId(organizationId)
                        .sequenceKey(sequenceKey)
                        .context(context)
                        .lastNumber(0L)
                        .build()));
        long next = counter.getLastNumber() + 1;
        counter.setLastNumber(next);
        repository.save(counter);
        return next;
    }

    private String format(SequenceCounter counter, long number) {
        StringBuilder sb = new StringBuilder();
        if (counter.getPrefix() != null && !counter.getPrefix().isBlank()) {
            sb.append(counter.getPrefix()).append('-');
        }
        if (counter.getContext() != null && !counter.getContext().isBlank()) {
            sb.append(counter.getContext()).append('-');
        }
        int padding = counter.getPadding() != null ? counter.getPadding() : 5;
        sb.append(String.format("%0" + padding + "d", number));
        return sb.toString();
    }

    private String defaultPrefix(String sequenceKey) {
        return switch (sequenceKey) {
            case "ADMISSION"   -> "ADM";
            case "ENROLLMENT"  -> "ENR";
            case "INVOICE"     -> "INV";
            case "RECEIPT"     -> "RCT";
            case "PAYMENT"     -> "PAY";
            case "REFUND"      -> "REF";
            case "EXAM"        -> "EXM";
            case "TRANSFER"    -> "TC";
            default            -> sequenceKey.length() > 3 ? sequenceKey.substring(0, 3) : sequenceKey;
        };
    }
}
