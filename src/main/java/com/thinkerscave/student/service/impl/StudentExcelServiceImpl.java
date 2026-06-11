package com.thinkerscave.student.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.thinkerscave.student.service.StudentExcelService;

@Service
public class StudentExcelServiceImpl implements StudentExcelService {

	@Override
    public Resource generateStudentImportTemplate() {

        try (Workbook workbook = new XSSFWorkbook()) {

            Sheet templateSheet = workbook.createSheet("Student Import");
            Sheet instructionSheet = workbook.createSheet("Instructions");

            createStudentImportHeader(templateSheet);
            createInstructionSheet(instructionSheet);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);

            return new ByteArrayResource(outputStream.toByteArray());

        } catch (IOException ex) {
            throw new RuntimeException("Failed to generate student import template", ex);
        }
    }

    private void createStudentImportHeader(Sheet sheet) {

        Row header = sheet.createRow(0);

        String[] columns = {
                "Admission Number *",
                "First Name *",
                "Middle Name",
                "Last Name *",
                "Gender *",
                "Date Of Birth *",
                "Mobile Number",
                "Email",
                "Academic Year *",
                "Class Code *",
                "Section Code *",
                "Roll Number",
                "Father Name *",
                "Father Mobile *",
                "Mother Name",
                "Mother Mobile",
                "Current Address",
                "Blood Group",
                "Remarks"
        };

        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
            sheet.autoSizeColumn(i);
        }
    }

    private void createInstructionSheet(Sheet sheet) {

        Row header = sheet.createRow(0);

        header.createCell(0).setCellValue("Field");
        header.createCell(1).setCellValue("Mandatory");
        header.createCell(2).setCellValue("Description");

        createInstructionRow(sheet, 1,
                "Admission Number", "Yes",
                "Unique student admission number");

        createInstructionRow(sheet, 2,
                "Gender", "Yes",
                "Allowed values: MALE,FEMALE,OTHER");

        createInstructionRow(sheet, 3,
                "Date Of Birth", "Yes",
                "Format: dd-MM-yyyy");

        createInstructionRow(sheet, 4,
                "Blood Group", "No",
                "A+,A-,B+,B-,AB+,AB-,O+,O-");

        createInstructionRow(sheet, 5,
                "Class Code", "Yes",
                "Must exist in system");

        createInstructionRow(sheet, 6,
                "Section Code", "Yes",
                "Must exist in system");
    }

    private void createInstructionRow(
            Sheet sheet,
            int rowNumber,
            String field,
            String mandatory,
            String description) {

        Row row = sheet.createRow(rowNumber);

        row.createCell(0).setCellValue(field);
        row.createCell(1).setCellValue(mandatory);
        row.createCell(2).setCellValue(description);
    }
}
