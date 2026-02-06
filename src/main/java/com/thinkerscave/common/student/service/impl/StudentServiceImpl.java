package com.thinkerscave.common.student.service.impl;

import com.thinkerscave.common.commonModel.Address;
import com.thinkerscave.common.menum.domain.Role;
import com.thinkerscave.common.menum.repository.RoleRepository;
import com.thinkerscave.common.student.domain.Guardian;
import com.thinkerscave.common.student.domain.Student;
import com.thinkerscave.common.student.dto.StudentRequestDTO;
import com.thinkerscave.common.student.repository.ClassRepository;
import com.thinkerscave.common.student.repository.GuardianRepository;
import com.thinkerscave.common.student.repository.SectionRepository;
import com.thinkerscave.common.student.repository.StudentRepository;
import com.thinkerscave.common.student.service.StudentService;
import com.thinkerscave.common.usrm.dto.UserCreationContext;
import com.thinkerscave.common.usrm.service.UserService;
import com.thinkerscave.common.usrm.domain.User;
import jakarta.transaction.Transactional;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

	private static final String ROLE_USER = "USER";
	private static final String TYPE_STUDENT = "student";
	private static final String TYPE_GUARDIAN = "guardian";

	private final Path rootLocation = Paths.get("uploads");

	private final UserService userService;
	private final RoleRepository roleRepository;
	private final GuardianRepository guardianRepository;
	private final StudentRepository studentRepository;
	private final ClassRepository classRepository;
	private final SectionRepository sectionRepository;

	@PostConstruct
	public void init() {
		try {
			if (!Files.exists(rootLocation)) {
				Files.createDirectories(rootLocation);
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not initialize storage", e);
		}
	}

	@Transactional
	public Student saveStudentWithDocuments(StudentRequestDTO dto,
			MultipartFile photo,
			List<MultipartFile> documents,
			List<String> types) {
		try {
			// --- Step 1: Fetch Role ---
			Role userRole = roleRepository.findByRoleName(ROLE_USER)
					.orElseThrow(() -> new IllegalStateException("Role '" + ROLE_USER + "' not found"));

			// --- Step 2: Create and Save Student User ---
			UserCreationContext studentContext = new UserCreationContext(
					dto.getFirstName(), dto.getMiddleName(), dto.getLastName(),
					dto.getEmail(), dto.getMobileNumber(),
					dto.getPermanentAddressLine(), dto.getPermanentState(), dto.getPermanentCity(),
					TYPE_STUDENT);
			User studentUser = userService.createUser(studentContext, userRole);

			// --- Step 3: Create and Save Guardian User ---
			UserCreationContext guardianContext = new UserCreationContext(
					dto.getGuardianFirstName(), dto.getGuardianMiddleName(), dto.getGuardianLastName(),
					dto.getGuardianEmail(), dto.getGuardianPhoneNumber(),
					dto.getGuardianAddress(), dto.getPermanentState(), dto.getPermanentCity(),
					TYPE_GUARDIAN);
			User guardianUser = userService.createUser(guardianContext, userRole);

			// --- Step 4: Save Guardian Entity ---
			Guardian guardian;
			try {
				guardian = new Guardian();
				guardian.setFirstName(dto.getGuardianFirstName());
				guardian.setMiddleName(dto.getGuardianMiddleName());
				guardian.setLastName(dto.getGuardianLastName());
				guardian.setEmail(dto.getGuardianEmail());
				guardian.setMobileNumber(Long.parseLong(dto.getGuardianPhoneNumber()));
				guardian.setRelation(dto.getGuardianRelation());
				guardian.setAddress(dto.getGuardianAddress());
				guardian.setUser(guardianUser);

				guardian = guardianRepository.save(guardian);
				guardian = guardianRepository.save(guardian);
			} catch (Exception e) {
				log.error("Failed to save guardian entity: {}", e.getMessage(), e);
				throw new RuntimeException("Failed to save guardian information.");
			}

			// --- Step 5: Save Student Entity ---
			Student student;
			try {
				student = new Student();
				student.setFirstName(dto.getFirstName());
				student.setMiddleName(dto.getMiddleName());
				student.setLastName(dto.getLastName());
				student.setEmail(dto.getEmail());
				student.setMobileNumber(Long.parseLong(dto.getMobileNumber()));
				student.setGender(dto.getGender());
				student.setDateOfBirth(dto.getDateOfBirth());
				student.setEnrollmentDate(dto.getEnrollmentDate());
				student.setRollNumber(dto.getRollNumber());
				student.setRemarks(dto.getRemarks());
				// student.setAge(Long.parseLong(dto.getAge()));
				student.setAge(10L);

				Address current = new Address();
				current.setCountry(dto.getCurrentCountry());
				current.setState(dto.getCurrentState());
				current.setCity(dto.getCurrentCity());
				current.setZipCode(dto.getCurrentZipCode());
				current.setAddressLine(dto.getCurrentAddressLine());
				log.debug("IN dto same address {}", dto.getIsSameAddress());

				Address permanent = dto.getIsSameAddress() ? current : new Address();
				if (!dto.getIsSameAddress()) {

					permanent.setCountry(dto.getPermanentCountry());
					permanent.setState(dto.getPermanentState());
					permanent.setCity(dto.getPermanentCity());
					permanent.setZipCode(dto.getPermanentZipCode());
					permanent.setAddressLine(dto.getPermanentAddressLine());
				}
				student.setSameAddress(dto.getIsSameAddress());
				student.setCurrentAddress(current);
				student.setPermanentAddress(permanent);
				student.setUser(studentUser);
				student.setParent(guardian);
				student.setActive(true);

				classRepository.findById(dto.getClassId()).ifPresent(student::setClassEntity);
				sectionRepository.findById(dto.getSectionId()).ifPresent(student::setSection);

				if (photo != null && !photo.isEmpty()) {
					String photoPath = saveFile(photo, "photo_" + dto.getEmail());
					student.setPhotoUrl(photoPath);
				}

				student = studentRepository.save(student);

				if (documents != null) {
					log.debug("Processing {} documents", documents.size());
					for (int i = 0; i < documents.size(); i++) {
						MultipartFile doc = documents.get(i);
						String type = types.get(i);
						log.debug("Processing document type: {}", type);
						saveFile(doc, "doc_" + type + "_" + dto.getEmail());
					}
				}

			} catch (Exception e) {
				log.error("Failed to save student entity: {}", e.getMessage(), e);
				throw new RuntimeException("Failed to save student information.");
			}

			return student;

		} catch (Exception e) {
			log.error("Transaction failed: {}", e.getMessage(), e);
			throw e; // This will trigger rollback
		}
	}

	private String saveFile(MultipartFile file, String prefix) throws IOException {
		log.debug("Saving file with prefix: {}", prefix);
		if (file == null || file.isEmpty())
			return "File is null";

		log.info("In save Document");
		Path destination = rootLocation.resolve(prefix).normalize().toAbsolutePath();

		// Security check
		if (!destination.getParent().equals(rootLocation.toAbsolutePath())) {
			log.info("In save Document 1");
			throw new IOException("Cannot store file outside current directory.");
		}

		try (var inputStream = file.getInputStream()) {

			Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
			return destination.toString();
		}
	}

}
