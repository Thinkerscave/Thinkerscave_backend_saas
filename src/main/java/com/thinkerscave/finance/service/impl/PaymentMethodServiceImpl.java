package com.thinkerscave.finance.service.impl;

import com.thinkerscave.audit.enums.AuditEventType;
import com.thinkerscave.audit.service.AuditWriteService;
import com.thinkerscave.finance.dto.request.PaymentMethodRequest;
import com.thinkerscave.finance.dto.request.StatusUpdateRequest;
import com.thinkerscave.finance.dto.response.PaymentMethodResponse;
import com.thinkerscave.finance.entity.FeePaymentMethod;
import com.thinkerscave.finance.enums.FeeMasterStatus;
import com.thinkerscave.finance.repository.FeePaymentMethodRepository;
import com.thinkerscave.finance.security.FinanceAccessGuard;
import com.thinkerscave.finance.service.PaymentMethodService;
import com.thinkerscave.shared.exceptions.AlreadyExistsException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentMethodServiceImpl implements PaymentMethodService {

    private final FeePaymentMethodRepository repository;
    private final FinanceAccessGuard accessGuard;
    private final AuditWriteService auditWriteService;

    @Override
    public List<PaymentMethodResponse> list(FeeMasterStatus status) {
        // Settings VIEW sees all; collection callers pass ACTIVE
        if (!accessGuard.canView(FinanceAccessGuard.RESOURCE_SETTINGS)
                && !accessGuard.canManage(FinanceAccessGuard.RESOURCE_COLLECTION)) {
            accessGuard.requireView(FinanceAccessGuard.RESOURCE_SETTINGS);
        }
        List<FeePaymentMethod> methods = status == null
                ? repository.findAllByOrderBySortOrderAscNameAsc()
                : repository.findByStatusOrderBySortOrderAscNameAsc(status);
        return methods.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public PaymentMethodResponse create(PaymentMethodRequest request) {
        accessGuard.requireManage(FinanceAccessGuard.RESOURCE_SETTINGS);
        String normalized = request.getName().trim().toLowerCase();
        if (repository.existsByNameNormalized(normalized)) {
            throw new AlreadyExistsException("Payment method name already exists", "name");
        }
        FeePaymentMethod m = new FeePaymentMethod();
        apply(m, request, normalized);
        FeePaymentMethod saved = repository.save(m);
        auditWriteService.record(AuditEventType.CREATE, "FEE_PAYMENT_METHOD_CREATE", "FeePaymentMethod",
                String.valueOf(saved.getFeePaymentMethodId()), "Created payment method");
        return toResponse(saved);
    }

    @Override
    @Transactional
    public PaymentMethodResponse update(Long id, PaymentMethodRequest request) {
        accessGuard.requireManage(FinanceAccessGuard.RESOURCE_SETTINGS);
        FeePaymentMethod m = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment method not found"));
        String normalized = request.getName().trim().toLowerCase();
        repository.findByNameNormalized(normalized).ifPresent(existing -> {
            if (!existing.getFeePaymentMethodId().equals(id)) {
                throw new AlreadyExistsException("Payment method name already exists", "name");
            }
        });
        apply(m, request, normalized);
        return toResponse(repository.save(m));
    }

    @Override
    @Transactional
    public PaymentMethodResponse updateStatus(Long id, StatusUpdateRequest request) {
        accessGuard.requireManage(FinanceAccessGuard.RESOURCE_SETTINGS);
        FeePaymentMethod m = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment method not found"));
        m.setStatus(request.getStatus());
        return toResponse(repository.save(m));
    }

    private void apply(FeePaymentMethod m, PaymentMethodRequest request, String normalized) {
        m.setName(request.getName().trim());
        m.setNameNormalized(normalized);
        m.setDescription(request.getDescription());
        m.setStatus(request.getStatus() == null ? FeeMasterStatus.ACTIVE : request.getStatus());
        m.setRequiresReference(Boolean.TRUE.equals(request.getRequiresReference()));
        m.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
    }

    private PaymentMethodResponse toResponse(FeePaymentMethod m) {
        return PaymentMethodResponse.builder()
                .feePaymentMethodId(m.getFeePaymentMethodId())
                .name(m.getName())
                .description(m.getDescription())
                .status(m.getStatus())
                .requiresReference(Boolean.TRUE.equals(m.getRequiresReference()))
                .sortOrder(m.getSortOrder())
                .build();
    }
}
