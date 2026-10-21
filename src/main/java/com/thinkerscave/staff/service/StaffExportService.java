package com.thinkerscave.staff.service;

import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.enums.EmploymentCategory;
import com.thinkerscave.staff.enums.EmploymentStatus;
import com.thinkerscave.staff.enums.StaffType;
import com.thinkerscave.staff.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Builds the staff directory as an Excel workbook using the same filters as the staff list. */
@Service
@RequiredArgsConstructor
public class StaffExportService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
    private static final String[] HEADERS = {
            "Staff Code", "Full Name", "Email", "Mobile", "Staff Type", "Designation",
            "Employment Category", "Employment Status", "Joining Date", "Active"
    };

    private final StaffRepository staffRepository;

    @Transactional(readOnly = true)
    public byte[] exportExcel(StaffType staffType, EmploymentCategory employmentCategory,
                              EmploymentStatus employmentStatus, String designation, String keyword) {
        List<Staff> rows = staffRepository.searchStaff(
                staffType, employmentCategory, employmentStatus, designation, keyword, Pageable.unpaged())
                .getContent();
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Staff");
            CellStyle headerStyle = workbook.createCellStyle();
            Font bold = workbook.createFont();
            bold.setBold(true);
            headerStyle.setFont(bold);

            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
                header.getCell(i).setCellStyle(headerStyle);
            }
            int rowIndex = 1;
            for (Staff s : rows) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(nullToEmpty(s.getStaffCode()));
                row.createCell(1).setCellValue((s.getFirstName() + " " + s.getLastName()).trim());
                row.createCell(2).setCellValue(nullToEmpty(s.getEmail()));
                row.createCell(3).setCellValue(nullToEmpty(s.getMobileNumber()));
                row.createCell(4).setCellValue(s.getStaffType() != null ? s.getStaffType().name() : "");
                row.createCell(5).setCellValue(nullToEmpty(s.getDesignation()));
                row.createCell(6).setCellValue(s.getEmploymentCategory() != null ? s.getEmploymentCategory().name() : "");
                row.createCell(7).setCellValue(s.getEmploymentStatus() != null ? s.getEmploymentStatus().name() : "");
                row.createCell(8).setCellValue(s.getJoiningDate() != null ? s.getJoiningDate().format(DATE) : "");
                row.createCell(9).setCellValue(Boolean.TRUE.equals(s.getActive()) ? "Yes" : "No");
            }
            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to build staff export: " + ex.getMessage(), ex);
        }
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}
