package com.thinkerscave.common.student.service.impl;

import org.springframework.transaction.annotation.Transactional;
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
import com.thinkerscave.common.student.domain.StudentDocument;
import com.thinkerscave.common.student.dto.StudentDocumentDTO;
import com.thinkerscave.common.student.repository.StudentDocumentRepository;
import com.thinkerscave.common.student.service.StudentService;
import com.thinkerscave.common.usrm.dto.UserCreationContext;
import com.thinkerscave.common.usrm.service.UserService;
import com.thinkerscave.common.usrm.domain.User;
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
@Transactional(readOnly = true)
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
	private final StudentDocumentRepository studentDocumentRepository;

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
	public com.thinkerscave.common.student.dto.StudentResponseDTO saveStudentWithDocuments(StudentRequestDTO dto,
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

				if (dto.getDateOfBirth() != null) {
					long calculatedAge = java.time.temporal.ChronoUnit.YEARS.between(dto.getDateOfBirth(),
							java.time.LocalDate.now());
					student.setAge(calculatedAge);
				} else {
					student.setAge(0L); // Optional fallback if DOB is somehow missing
				}

				// Set organization scope from context
				Long orgId = com.thinkerscave.common.context.OrganizationContext.getOrganizationId();
				if (orgId != null) {
					student.setOrganizationId(orgId);
				}

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
						String path = saveFile(doc, "doc_" + type + "_" + dto.getEmail());

						StudentDocument studentDocument = new StudentDocument();
						studentDocument.setDocumentName(doc.getOriginalFilename());
						studentDocument.setDocumentType(type);
						studentDocument.setDocumentPath(path);
						studentDocument.setStudent(student);
						studentDocument.setOrganizationId(orgId);
						studentDocumentRepository.save(studentDocument);
					}
				}

			} catch (Exception e) {
				log.error("Failed to save student entity: {}", e.getMessage(), e);
				throw new RuntimeException("Failed to save student information.");
			}

			return mapToResponseDTO(student);

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

	@Override
	public List<com.thinkerscave.common.student.dto.StudentResponseDTO> getAllStudents() {
		Long orgId = com.thinkerscave.common.context.OrganizationContext.getOrganizationId();
		List<Student> students = studentRepository.findByOrganizationIdAndIsActive(orgId, true);
		return students.stream().map(this::mapToResponseDTO).collect(java.util.stream.Collectors.toList());
	}

	@Override
	public com.thinkerscave.common.student.dto.StudentResponseDTO getStudentById(Long id) {
		Long orgId = com.thinkerscave.common.context.OrganizationContext.getOrganizationId();
		Student student = studentRepository.findByStudentIdAndOrganizationId(id, orgId)
				.orElseThrow(() -> new RuntimeException("Student not found"));
		return mapToResponseDTO(student);
	}

	@Override
	@Transactional
	public com.thinkerscave.common.student.dto.StudentResponseDTO updateStudent(Long id, StudentRequestDTO dto) {
		Long orgId = com.thinkerscave.common.context.OrganizationContext.getOrganizationId();
		Student student = studentRepository.findByStudentIdAndOrganizationId(id, orgId)
				.orElseThrow(() -> new RuntimeException("Student not found"));

		student.setFirstName(dto.getFirstName());
		student.setMiddleName(dto.getMiddleName());
		student.setLastName(dto.getLastName());
		if (dto.getMobileNumber() != null) {
			student.setMobileNumber(Long.parseLong(dto.getMobileNumber()));
		}
		student.setGender(dto.getGender());
		student.setDateOfBirth(dto.getDateOfBirth());
		if (dto.getDateOfBirth() != null) {
			long calculatedAge = java.time.temporal.ChronoUnit.YEARS.between(dto.getDateOfBirth(),
					java.time.LocalDate.now());
			student.setAge(calculatedAge);
		}
		student.setEnrollmentDate(dto.getEnrollmentDate());
		student.setRollNumber(dto.getRollNumber());
		student.setRemarks(dto.getRemarks());

		if (dto.getClassId() != null) {
			classRepository.findById(dto.getClassId()).ifPresent(student::setClassEntity);
		}
		if (dto.getSectionId() != null) {
			sectionRepository.findById(dto.getSectionId()).ifPresent(student::setSection);
		}

		return mapToResponseDTO(studentRepository.save(student));
	}

	@Override
	@Transactional
	public void deleteStudent(Long id) {
		Long orgId = com.thinkerscave.common.context.OrganizationContext.getOrganizationId();
		Student student = studentRepository.findByStudentIdAndOrganizationId(id, orgId)
				.orElseThrow(() -> new RuntimeException("Student not found"));
		student.setActive(false);
		studentRepository.save(student);
	}

	private com.thinkerscave.common.student.dto.StudentResponseDTO mapToResponseDTO(Student student) {
		com.thinkerscave.common.student.dto.StudentResponseDTO dto = new com.thinkerscave.common.student.dto.StudentResponseDTO();
		dto.setStudentId(student.getStudentId());
		dto.setFirstName(student.getFirstName());
		dto.setLastName(student.getLastName());
		dto.setMiddleName(student.getMiddleName());
		dto.setEmail(student.getEmail());
		dto.setMobileNumber(student.getMobileNumber());
		dto.setGender(student.getGender());
		dto.setDateOfBirth(student.getDateOfBirth());
		dto.setRollNumber(student.getRollNumber());
		dto.setEnrollmentDate(student.getEnrollmentDate());
		dto.setActive(student.isActive());
		dto.setRemarks(student.getRemarks());

		if (student.getClassEntity() != null) {
			dto.setClassName(student.getClassEntity().getClassName());
			dto.setClassId(student.getClassEntity().getClassId());
		}
		if (student.getSection() != null) {
			dto.setSectionName(student.getSection().getSectionName());
			dto.setSectionId(student.getSection().getSectionId());
		}
		if (student.getParent() != null) {
			String parentFirstName = student.getParent().getFirstName() != null ? student.getParent().getFirstName()
					: "";
			String parentLastName = student.getParent().getLastName() != null ? student.getParent().getLastName() : "";
			dto.setParentName((parentFirstName + " " + parentLastName).trim());
		}
		return dto;
	}

	@Override
	public List<StudentDocumentDTO> getStudentDocuments(Long studentId) {
		Long orgId = com.thinkerscave.common.context.OrganizationContext.getOrganizationId();
		List<StudentDocument> docs = studentDocumentRepository.findByStudentStudentIdAndOrganizationId(studentId,
				orgId);
		return docs.stream().map(doc -> {
			StudentDocumentDTO dto = new StudentDocumentDTO();
			dto.setDocumentId(doc.getDocumentId());
			dto.setDocumentName(doc.getDocumentName());
			dto.setDocumentType(doc.getDocumentType());
			return dto;
		}).collect(java.util.stream.Collectors.toList());
	}

	@Override
	public org.springframework.core.io.Resource downloadDocument(Long documentId) {
		Long orgId = com.thinkerscave.common.context.OrganizationContext.getOrganizationId();
		StudentDocument doc = studentDocumentRepository.findByDocumentIdAndOrganizationId(documentId, orgId)
				.orElseThrow(() -> new RuntimeException("Document not found"));

		try {
			Path file = Paths.get(doc.getDocumentPath());
			org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(file.toUri());
			if (resource.exists() || resource.isReadable()) {
				return resource;
			} else {
				throw new RuntimeException("Could not read file!");
			}
		} catch (java.net.MalformedURLException e) {
			throw new RuntimeException("Error: " + e.getMessage());
		}
	}

}
