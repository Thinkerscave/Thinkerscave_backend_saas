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
        Long orgId = OrganizationContext.getOrganizationId();
        long totalLeads = inquiryRepository.countByOrganizationIdAndDeletedFalse(orgId);
        long interested = inquiryRepository.countByOrganizationIdAndStatusAndDeletedFalse(orgId, InquiryStatus.INTERESTED);
        long submitted = applicationRepository.countByOrganizationIdAndStatus(orgId, ApplicationStatus.SUBMITTED);
        long approved = applicationRepository.countByOrganizationIdAndStatus(orgId, ApplicationStatus.APPROVED);

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
        Long orgId = OrganizationContext.getOrganizationId();
        Map<String, Long> funnel = new LinkedHashMap<>();
        funnel.put("NEW", inquiryRepository.countByOrganizationIdAndStatusAndDeletedFalse(orgId, InquiryStatus.NEW));
        funnel.put("CONTACTED", inquiryRepository.countByOrganizationIdAndStatusAndDeletedFalse(orgId, InquiryStatus.CONTACTED));
        funnel.put("INTERESTED", inquiryRepository.countByOrganizationIdAndStatusAndDeletedFalse(orgId, InquiryStatus.INTERESTED));
        funnel.put("APPLICATION_SUBMITTED", applicationRepository.countByOrganizationIdAndStatus(orgId, ApplicationStatus.SUBMITTED));
        funnel.put("APPROVED", applicationRepository.countByOrganizationIdAndStatus(orgId, ApplicationStatus.APPROVED));
        funnel.put("ENROLLED", applicationRepository.countByOrganizationIdAndStatus(orgId, ApplicationStatus.ENROLLED));
        return funnel;
    }

    @Override
    public List<Map<String, Object>> counselorPerformance() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object[] row : inquiryRepository.countByCounselorForOrg(OrganizationContext.getOrganizationId())) {
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
        for (Object[] row : inquiryRepository.countBySourceForOrg(OrganizationContext.getOrganizationId())) {
            source.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }
        return source;
    }
}