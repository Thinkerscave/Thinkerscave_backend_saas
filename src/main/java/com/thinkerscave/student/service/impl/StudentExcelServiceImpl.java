package com.thinkerscave.student.service.impl;

import java.io.ByteArrayInputStream;
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
import org.springframework.web.multipart.MultipartFile;

import com.thinkerscave.shared.exceptions.FileProcessingException;
import com.thinkerscave.student.service.StudentExcelService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class StudentExcelServiceImpl implements StudentExcelService {

	@Override
	public ByteArrayInputStream downloadTemplate() {

		try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

			Sheet templateSheet = workbook.createSheet("Student Import");
			Sheet instructionSheet = workbook.createSheet("Instructions");

			createStudentImportHeader(templateSheet);
			createInstructionSheet(instructionSheet);

			workbook.write(outputStream);

			return new ByteArrayInputStream(outputStream.toByteArray());

		} catch (IOException ex) {

			log.error("Failed to generate student import template", ex);

			throw new FileProcessingException("Unable to generate student import template", ex);
		}
	}

	private void createStudentImportHeader(Sheet sheet) {

		Row header = sheet.createRow(0);

		String[] columns = { "Admission Number *", "First Name *", "Middle Name", "Last Name *", "Gender *",
				"Date Of Birth *", "Mobile Number", "Email", "Academic Year *", "Class Code *", "Section Code *",
				"Roll Number", "Father Name *", "Father Mobile *", "Mother Name", "Mother Mobile", "Current Address",
				"Blood Group", "Remarks" };

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

		createInstructionRow(sheet, 1, "Admission Number", "Yes", "Unique student admission number");

		createInstructionRow(sheet, 2, "Gender", "Yes", "Allowed values: MALE,FEMALE,OTHER");

		createInstructionRow(sheet, 3, "Date Of Birth", "Yes", "Format: dd-MM-yyyy");

		createInstructionRow(sheet, 4, "Blood Group", "No", "A+,A-,B+,B-,AB+,AB-,O+,O-");

		createInstructionRow(sheet, 5, "Class Code", "Yes", "Must exist in system");

		createInstructionRow(sheet, 6, "Section Code", "Yes", "Must exist in system");
	}

	private void createInstructionRow(Sheet sheet, int rowNumber, String field, String mandatory, String description) {

		Row row = sheet.createRow(rowNumber);

		row.createCell(0).setCellValue(field);
		row.createCell(1).setCellValue(mandatory);
		row.createCell(2).setCellValue(description);
	}

	@Override
	public Resource importStudents(MultipartFile file) {
		// TODO Auto-generated method stub
		return null;
	}

}
