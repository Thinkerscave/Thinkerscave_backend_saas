package com.thinkerscave.staff.service;

import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.staff.dto.request.StaffBankDetailsRequest;
import com.thinkerscave.staff.dto.response.StaffBankDetailsResponse;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.entity.StaffBankDetails;
import com.thinkerscave.staff.enums.StaffActivityType;
import com.thinkerscave.staff.repository.StaffBankDetailsRepository;
import com.thinkerscave.staff.repository.StaffRepository;
import com.thinkerscave.staff.security.StaffBankAccountCipher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Stores staff bank details. The account number is encrypted before it reaches the entity;
 * if encryption is unavailable the write fails and no bank row is created.
 */
@Service
@RequiredArgsConstructor
public class StaffBankDetailsService {

    private final StaffBankDetailsRepository bankRepository;
    private final StaffRepository staffRepository;
    private final StaffBankAccountCipher cipher;
    private final StaffActivityService activityService;

    /** Creates or replaces the bank record for a staff member. */
    @Transactional
    public StaffBankDetailsResponse upsert(Long staffId, StaffBankDetailsRequest request) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found with ID: " + staffId));
        return toResponse(apply(staff, request));
    }

    /** Applies bank details to a staff member that is already persisted (used during create). */
    @Transactional
    public StaffBankDetails apply(Staff staff, StaffBankDetailsRequest request) {
        String encrypted = cipher.encrypt(request.getAccountNumber().trim());
        String digits = request.getAccountNumber().trim();

        StaffBankDetails details = bankRepository.findByStaff_StaffId(staff.getStaffId())
                .orElseGet(StaffBankDetails::new);
        details.setStaff(staff);
        details.setBankName(request.getBankName().trim());
        details.setAccountHolderName(request.getAccountHolderName().trim());
        details.setAccountNumberEncrypted(encrypted);
        details.setAccountNumberLast4(digits.substring(digits.length() - 4));
        details.setIfscCode(request.getIfscCode().trim().toUpperCase());
        details.setEncryptionKeyVersion(StaffBankAccountCipher.KEY_VERSION);
        details.setActive(true);
        StaffBankDetails saved = bankRepository.save(details);
        activityService.record(staff, StaffActivityType.BANK_DETAILS_UPDATED, "Bank details", "Bank details saved");
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<StaffBankDetailsResponse> findMasked(Long staffId) {
        return bankRepository.findByStaff_StaffId(staffId).map(this::toResponse);
    }

    public StaffBankDetailsResponse toResponse(StaffBankDetails details) {
        return StaffBankDetailsResponse.builder()
                .staffId(details.getStaff().getStaffId())
                .bankName(details.getBankName())
                .accountHolderName(details.getAccountHolderName())
                .maskedAccountNumber("XXXX XXXX " + details.getAccountNumberLast4())
                .ifscCode(details.getIfscCode())
                .updatedOn(details.getUpdatedOn())
                .build();
    }
}
