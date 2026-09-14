package com.thinkerscave.finance.service.impl;

import com.thinkerscave.finance.entity.FeeReceiptSequence;
import com.thinkerscave.finance.repository.FeeReceiptSequenceRepository;
import com.thinkerscave.finance.service.FeeReceiptNumberService;
import com.thinkerscave.platform.entity.OrganizationConfiguration;
import com.thinkerscave.platform.repository.OrganizationConfigurationRepository;
import com.thinkerscave.shared.context.OrganizationContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class FeeReceiptNumberServiceImpl implements FeeReceiptNumberService {

    private static final String SEQ_KEY = "FEE_RECEIPT";
    private static final Pattern SEQ_PATTERN = Pattern.compile("\\{SEQ:(\\d+)\\}");

    private final FeeReceiptSequenceRepository sequenceRepository;
    private final OrganizationConfigurationRepository organizationConfigurationRepository;

    @Override
    @Transactional
    public String nextReceiptNumber() {
        FeeReceiptSequence seq = sequenceRepository.findBySequenceKeyForUpdate(SEQ_KEY)
                .orElseGet(() -> {
                    FeeReceiptSequence created = new FeeReceiptSequence();
                    created.setSequenceKey(SEQ_KEY);
                    created.setLastValue(0L);
                    return sequenceRepository.save(created);
                });
        long next = (seq.getLastValue() == null ? 0L : seq.getLastValue()) + 1L;
        seq.setLastValue(next);
        sequenceRepository.save(seq);

        String pattern = organizationConfigurationRepository
                .findByOrganization_Id(OrganizationContext.getOrganizationId())
                .map(OrganizationConfiguration::getReceiptNumberPattern)
                .filter(p -> p != null && !p.isBlank())
                .orElse(null);

        if (pattern == null) {
            return String.format("REC-%d-%06d", LocalDate.now().getYear(), next);
        }
        String out = pattern.replace("{YYYY}", String.valueOf(LocalDate.now().getYear()));
        Matcher m = SEQ_PATTERN.matcher(out);
        if (m.find()) {
            int width = Integer.parseInt(m.group(1));
            out = m.replaceFirst(String.format("%0" + width + "d", next));
        } else {
            out = out + String.format("%06d", next);
        }
        return out;
    }
}
