package com.thinkerscave.staff.service.impl;

import com.thinkerscave.access.repository.UserRepository;
import com.thinkerscave.shared.context.TenantContext;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.staff.dto.request.PayrollGenerateRequest;
import com.thinkerscave.staff.dto.response.PayrollGenerateResult;
import com.thinkerscave.staff.entity.Payroll;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.entity.StaffSalaryStructure;
import com.thinkerscave.staff.enums.EmploymentStatus;
import com.thinkerscave.staff.enums.PayrollStatus;
import com.thinkerscave.staff.repository.PayrollRepository;
import com.thinkerscave.staff.repository.StaffRepository;
import com.thinkerscave.staff.repository.StaffSalaryStructureRepository;
import com.thinkerscave.staff.service.PayslipPdfService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PayrollServiceImpl tenant-scoped generation")
class PayrollServiceImplTest {

    private static final String TENANT = "tenant_cp20260724113915";

    @Mock
    private PayrollRepository payrollRepository;
    @Mock
    private StaffRepository staffRepository;
    @Mock
    private StaffSalaryStructureRepository salaryStructureRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PayslipPdfService payslipPdfService;

    @InjectMocks
    private PayrollServiceImpl payrollService;

    @BeforeEach
    void setUp() {
        TenantContext.setTenant(TENANT);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("rejects platform/public tenant — never generate against platform catalog")
    void rejectsPublicTenant() {
        TenantContext.setTenant("public");
        assertThrows(BadRequestException.class, () -> payrollService.generatePayroll(request(2026, 7)));
        verifyNoInteractions(staffRepository, payrollRepository, salaryStructureRepository);
    }

    @Test
    @DisplayName("rejects blank tenant context")
    void rejectsBlankTenant() {
        TenantContext.clear();
        assertThrows(BadRequestException.class, () -> payrollService.generatePayroll(request(2026, 7)));
        verifyNoInteractions(staffRepository);
    }

    @Test
    @DisplayName("loads staff via schema-scoped active query (not findAll)")
    void usesActiveStaffQueryNotFindAll() {
        when(staffRepository.findByActiveTrueAndEmploymentStatus(EmploymentStatus.ACTIVE))
                .thenReturn(List.of());

        PayrollGenerateResult result = payrollService.generatePayroll(request(2026, 8));

        verify(staffRepository).findByActiveTrueAndEmploymentStatus(EmploymentStatus.ACTIVE);
        verify(staffRepository, never()).findAll();
        assertEquals(0, result.getGeneratedRecords());
        assertEquals(TENANT, result.getTenantIdentifier());
    }

    @Test
    @DisplayName("creates payroll for staff with salary structure")
    void generatesForEligibleStaff() {
        Staff staff = staff(1L, "STF001");
        StaffSalaryStructure structure = structure(new BigDecimal("50000.00"));

        when(staffRepository.findByActiveTrueAndEmploymentStatus(EmploymentStatus.ACTIVE))
                .thenReturn(List.of(staff));
        when(payrollRepository.existsByStaff_StaffIdAndPayrollYearAndPayrollMonth(1L, 2026, 8))
                .thenReturn(false);
        when(salaryStructureRepository.findByStaff_StaffIdAndActiveTrue(1L))
                .thenReturn(Optional.of(structure));
        when(payrollRepository.save(any(Payroll.class))).thenAnswer(inv -> {
            Payroll p = inv.getArgument(0);
            p.setPayrollId(99L);
            return p;
        });

        PayrollGenerateResult result = payrollService.generatePayroll(request(2026, 8));

        assertEquals(1, result.getGeneratedRecords());
        assertEquals(0, result.getSkippedAlreadyExists());
        assertTrue(result.getSkippedNoSalaryStructure().isEmpty());

        ArgumentCaptor<Payroll> captor = ArgumentCaptor.forClass(Payroll.class);
        verify(payrollRepository).save(captor.capture());
        Payroll saved = captor.getValue();
        assertEquals(staff, saved.getStaff());
        assertEquals(2026, saved.getPayrollYear());
        assertEquals(8, saved.getPayrollMonth());
        assertEquals(PayrollStatus.GENERATED, saved.getStatus());
        assertEquals(0, saved.getGrossSalary().compareTo(new BigDecimal("50000.00")));
        assertEquals(0, saved.getNetSalary().compareTo(new BigDecimal("50000.00")));
        assertEquals(0, saved.getTotalDeductions().compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("applies PF/ESI/PT deductions into net salary")
    void appliesStatutoryDeductions() {
        Staff staff = staff(1L, "STF001");
        StaffSalaryStructure structure = structure(new BigDecimal("50000.00"));
        structure.setPfEmployee(new BigDecimal("1800"));
        structure.setEsiEmployee(new BigDecimal("375"));
        structure.setProfessionalTax(new BigDecimal("200"));
        structure.setOtherDeduction(BigDecimal.ZERO);

        when(staffRepository.findByActiveTrueAndEmploymentStatus(EmploymentStatus.ACTIVE))
                .thenReturn(List.of(staff));
        when(payrollRepository.existsByStaff_StaffIdAndPayrollYearAndPayrollMonth(1L, 2026, 8))
                .thenReturn(false);
        when(salaryStructureRepository.findByStaff_StaffIdAndActiveTrue(1L))
                .thenReturn(Optional.of(structure));
        when(payrollRepository.save(any(Payroll.class))).thenAnswer(inv -> inv.getArgument(0));

        PayrollGenerateResult result = payrollService.generatePayroll(request(2026, 8));
        assertEquals(1, result.getGeneratedRecords());

        ArgumentCaptor<Payroll> captor = ArgumentCaptor.forClass(Payroll.class);
        verify(payrollRepository).save(captor.capture());
        Payroll saved = captor.getValue();
        assertEquals(0, saved.getPfAmount().compareTo(new BigDecimal("1800")));
        assertEquals(0, saved.getTotalDeductions().compareTo(new BigDecimal("2375")));
        assertEquals(0, saved.getNetSalary().compareTo(new BigDecimal("47625")));
    }

    @Test
    @DisplayName("skips duplicate staff/month in same tenant schema")
    void skipsAlreadyExists() {
        Staff staff = staff(1L, "STF001");
        when(staffRepository.findByActiveTrueAndEmploymentStatus(EmploymentStatus.ACTIVE))
                .thenReturn(List.of(staff));
        when(payrollRepository.existsByStaff_StaffIdAndPayrollYearAndPayrollMonth(1L, 2026, 8))
                .thenReturn(true);

        PayrollGenerateResult result = payrollService.generatePayroll(request(2026, 8));

        assertEquals(0, result.getGeneratedRecords());
        assertEquals(1, result.getSkippedAlreadyExists());
        verify(payrollRepository, never()).save(any());
        verify(salaryStructureRepository, never()).findByStaff_StaffIdAndActiveTrue(anyLong());
    }

    @Test
    @DisplayName("surfaces staff codes missing salary structure (not silent skip)")
    void surfacesMissingSalaryStructure() {
        Staff staff = staff(2L, "STF-NOSAL");
        when(staffRepository.findByActiveTrueAndEmploymentStatus(EmploymentStatus.ACTIVE))
                .thenReturn(List.of(staff));
        when(payrollRepository.existsByStaff_StaffIdAndPayrollYearAndPayrollMonth(2L, 2026, 8))
                .thenReturn(false);
        when(salaryStructureRepository.findByStaff_StaffIdAndActiveTrue(2L))
                .thenReturn(Optional.empty());

        PayrollGenerateResult result = payrollService.generatePayroll(request(2026, 8));

        assertEquals(0, result.getGeneratedRecords());
        assertEquals(List.of("STF-NOSAL"), result.getSkippedNoSalaryStructure());
        verify(payrollRepository, never()).save(any());
    }

    @Test
    @DisplayName("mixed run: generate one, skip duplicate, skip no-salary")
    void mixedRun() {
        Staff ok = staff(1L, "OK");
        Staff dup = staff(2L, "DUP");
        Staff noSal = staff(3L, "NOSAL");

        when(staffRepository.findByActiveTrueAndEmploymentStatus(EmploymentStatus.ACTIVE))
                .thenReturn(List.of(ok, dup, noSal));
        when(payrollRepository.existsByStaff_StaffIdAndPayrollYearAndPayrollMonth(1L, 2026, 3))
                .thenReturn(false);
        when(payrollRepository.existsByStaff_StaffIdAndPayrollYearAndPayrollMonth(2L, 2026, 3))
                .thenReturn(true);
        when(payrollRepository.existsByStaff_StaffIdAndPayrollYearAndPayrollMonth(3L, 2026, 3))
                .thenReturn(false);
        when(salaryStructureRepository.findByStaff_StaffIdAndActiveTrue(1L))
                .thenReturn(Optional.of(structure(new BigDecimal("10000"))));
        when(salaryStructureRepository.findByStaff_StaffIdAndActiveTrue(3L))
                .thenReturn(Optional.empty());
        when(payrollRepository.save(any(Payroll.class))).thenAnswer(inv -> inv.getArgument(0));

        PayrollGenerateResult result = payrollService.generatePayroll(request(2026, 3));

        assertEquals(1, result.getGeneratedRecords());
        assertEquals(1, result.getSkippedAlreadyExists());
        assertEquals(List.of("NOSAL"), result.getSkippedNoSalaryStructure());
        assertEquals(TENANT, result.getTenantIdentifier());
        verify(payrollRepository, times(1)).save(any());
    }

    private static PayrollGenerateRequest request(int year, int month) {
        PayrollGenerateRequest r = new PayrollGenerateRequest();
        r.setYear(year);
        r.setMonth(month);
        return r;
    }

    private static Staff staff(Long id, String code) {
        Staff s = new Staff();
        s.setStaffId(id);
        s.setStaffCode(code);
        s.setFirstName("A");
        s.setLastName("B");
        s.setActive(true);
        s.setEmploymentStatus(EmploymentStatus.ACTIVE);
        return s;
    }

    private static StaffSalaryStructure structure(BigDecimal gross) {
        StaffSalaryStructure ss = new StaffSalaryStructure();
        ss.setGrossSalary(gross);
        ss.setActive(true);
        return ss;
    }
}
