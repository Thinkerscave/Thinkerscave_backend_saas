package com.thinkerscave.admission.service.impl;

import com.thinkerscave.admission.enums.ApplicationStatus;
import com.thinkerscave.admission.enums.InquiryStatus;
import com.thinkerscave.admission.repository.ApplicationAdmissionRepository;
import com.thinkerscave.admission.repository.InquiryRepository;
import com.thinkerscave.admission.service.AdmissionReportService;
import com.thinkerscave.shared.context.OrganizationContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdmissionReportServiceImpl implements AdmissionReportService {

    private final InquiryRepository inquiryRepository;
    private final ApplicationAdmissionRepository applicationRepository;

    @Override
    public Map<String, Object> overview() {
        long totalLeads = inquiryRepository.countByDeletedFalse();
        long interested = inquiryRepository.countByStatusAndDeletedFalse(InquiryStatus.INTERESTED);
        long submitted = applicationRepository.countByStatus(ApplicationStatus.SUBMITTED);
        long approved = applicationRepository.countByStatus(ApplicationStatus.APPROVED);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalLeads", totalLeads);
        response.put("interestedLeads", interested);
        response.put("submittedApplications", submitted);
        response.put("approvedApplications", approved);
        response.put("conversionRateInquiryToAdmission", totalLeads == 0 ? 0D : (approved * 100.0) / totalLeads);
        return response;
    }

    @Override
    public Map<String, Long> funnel() {
        Map<String, Long> funnel = new LinkedHashMap<>();
        funnel.put("NEW", inquiryRepository.countByStatusAndDeletedFalse(InquiryStatus.NEW));
        funnel.put("CONTACTED", inquiryRepository.countByStatusAndDeletedFalse(InquiryStatus.CONTACTED));
        funnel.put("INTERESTED", inquiryRepository.countByStatusAndDeletedFalse(InquiryStatus.INTERESTED));
        funnel.put("APPLICATION_SUBMITTED", applicationRepository.countByStatus(ApplicationStatus.SUBMITTED));
        funnel.put("APPROVED", applicationRepository.countByStatus(ApplicationStatus.APPROVED));
        funnel.put("ENROLLED", applicationRepository.countByStatus(ApplicationStatus.ENROLLED));
        return funnel;
    }

    @Override
    public List<Map<String, Object>> counselorPerformance() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object[] row : inquiryRepository.countByCounselor()) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("counselorId", row[0]);
            data.put("leadCount", ((Number) row[1]).longValue());
            rows.add(data);
        }
        return rows;
    }

    @Override
    public Map<String, Long> sourceAnalysis() {
        Map<String, Long> source = new LinkedHashMap<>();
        for (Object[] row : inquiryRepository.countBySource()) {
            source.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }
        return source;
    }
}