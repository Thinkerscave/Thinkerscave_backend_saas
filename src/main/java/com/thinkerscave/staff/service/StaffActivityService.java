package com.thinkerscave.staff.service;

import com.thinkerscave.staff.dto.response.StaffActivityResponse;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.entity.StaffActivityLog;
import com.thinkerscave.staff.enums.StaffActivityType;
import com.thinkerscave.staff.repository.StaffActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffActivityService {

    private final StaffActivityLogRepository repository;

    /** Joins the caller's transaction, so the log row commits or rolls back with the change. */
    @Transactional
    public void record(Staff staff, StaffActivityType type, String section, String description) {
        StaffActivityLog entry = new StaffActivityLog();
        entry.setStaff(staff);
        entry.setActionType(type);
        entry.setSection(section);
        entry.setDescription(description);
        entry.setPerformedBy(currentUsername());
        entry.setPerformedOn(LocalDateTime.now());
        repository.save(entry);
    }

    @Transactional(readOnly = true)
    public List<StaffActivityResponse> list(Long staffId) {
        return repository.findTop100ByStaff_StaffIdOrderByPerformedOnDescActivityIdDesc(staffId).stream()
                .map(e -> StaffActivityResponse.builder()
                        .activityId(e.getActivityId())
                        .actionType(e.getActionType())
                        .section(e.getSection())
                        .description(e.getDescription())
                        .performedBy(e.getPerformedBy())
                        .performedOn(e.getPerformedOn())
                        .build())
                .toList();
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }
}
