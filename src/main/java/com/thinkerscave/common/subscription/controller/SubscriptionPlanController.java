package com.thinkerscave.common.subscription.controller;

import com.thinkerscave.common.subscription.domain.SubscriptionPlan;
import com.thinkerscave.common.subscription.dto.SubscriptionPlanDTO;
import com.thinkerscave.common.subscription.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/subscription-plans")
@RequiredArgsConstructor
public class SubscriptionPlanController {

    private final SubscriptionPlanRepository repository;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public List<SubscriptionPlanDTO> list() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<SubscriptionPlanDTO> get(@PathVariable Long id) {
        return repository.findById(id).map(this::toDto).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<SubscriptionPlanDTO> create(@RequestBody SubscriptionPlanDTO dto) {
        if (dto.planCode() == null || dto.planCode().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (repository.existsByPlanCode(dto.planCode())) {
            return ResponseEntity.status(409).build();
        }
        SubscriptionPlan plan = new SubscriptionPlan();
        apply(plan, dto);
        return ResponseEntity.ok(toDto(repository.save(plan)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<SubscriptionPlanDTO> update(@PathVariable Long id, @RequestBody SubscriptionPlanDTO dto) {
        Optional<SubscriptionPlan> found = repository.findById(id);
        if (found.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        SubscriptionPlan plan = found.get();
        apply(plan, dto);
        return ResponseEntity.ok(toDto(repository.save(plan)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void apply(SubscriptionPlan plan, SubscriptionPlanDTO dto) {
        plan.setPlanCode(dto.planCode());
        plan.setPlanName(dto.planName());
        plan.setDescription(dto.description());
        plan.setMonthlyPrice(dto.monthlyPrice());
        plan.setAnnualPrice(dto.annualPrice());
        plan.setCurrency(dto.currency() == null ? "INR" : dto.currency());
        plan.setMaxStudents(dto.maxStudents());
        plan.setMaxStaff(dto.maxStaff());
        plan.setMaxUsers(dto.maxUsers());
        plan.setStorageGb(dto.storageGb());
        plan.setModulesIncluded(dto.modulesIncluded());
        plan.setSupportTier(dto.supportTier());
        plan.setHighlightColor(dto.highlightColor());
        plan.setFeatured(dto.featured() != null && dto.featured());
        plan.setActive(dto.active() == null || dto.active());
    }

    private SubscriptionPlanDTO toDto(SubscriptionPlan p) {
        return new SubscriptionPlanDTO(
                p.getPlanId(),
                p.getPlanCode(),
                p.getPlanName(),
                p.getDescription(),
                p.getMonthlyPrice(),
                p.getAnnualPrice(),
                p.getCurrency(),
                p.getMaxStudents(),
                p.getMaxStaff(),
                p.getMaxUsers(),
                p.getStorageGb(),
                p.getModulesIncluded(),
                p.getSupportTier(),
                p.getHighlightColor(),
                p.getFeatured(),
                p.getActive()
        );
    }
}
