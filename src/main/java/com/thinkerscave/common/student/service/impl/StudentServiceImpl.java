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
import com.thinkerscave.common.usrm.domain.User;
import com.thinkerscave.common.usrm.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
public class StudentServiceImpl implements StudentService{

	private static final Logger logger = LoggerFactory.getLogger(StudentServiceImpl.class);


	private final Path rootLocation = Paths.get("uploads");

	private  UserRepository userRepository;

	private  RoleRepository roleRepository;
	private GuardianRepository guardianRepository;
	private  StudentRepository studentRepository;
	private  ClassRepository classRepository;
	private  SectionRepository sectionRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	public StudentServiceImpl(UserRepository userRepository,
						  GuardianRepository guardianRepository,
						  StudentRepository studentRepository,
						  ClassRepository classRepository,
						  SectionRepository sectionRepository,RoleRepository roleRepository) throws IOException {

		this.userRepository = userRepository;
		this.guardianRepository = guardianRepository;
		this.studentRepository = studentRepository;
		this.classRepository = classRepository;
		this.sectionRepository = sectionRepository;
		this.roleRepository=roleRepository;
		// Create upload directory if it doesn't exist
		if (!Files.exists(rootLocation)) {
			Files.createDirectories(rootLocation);
		}
	}

	@Transactional
	public Student saveStudentWithDocuments(StudentRequestDTO dto,
											MultipartFile photo,
											List<MultipartFile> documents,
											List<String> types) {
		try {
			// --- Step 1: Fetch Role ---
			Role userRole = roleRepository.findByRoleName("USER")
					.orElseThrow(() -> new IllegalStateException("Role 'USER' not found"));

			// --- Step 2: Create and Save Student User ---
			User studentUser = new User();
			try {
				studentUser.setFirstName(dto.getFirstName());
				studentUser.setMiddleName(dto.getMiddleName());
				studentUser.setLastName(dto.getLastName());
				studentUser.setEmail(dto.getEmail());
				studentUser.setMobileNumber(Long.parseLong(dto.getMobileNumber()));
				studentUser.setAddress(dto.getPermanentAddressLine());
				studentUser.setState(dto.getPermanentState());
				studentUser.setCity(dto.getPermanentCity());

				String safeFirst = dto.getFirstName() != null ? dto.getFirstName().trim().toLowerCase() : "user";
				String safeLast = dto.getLastName() != null ? dto.getLastName().trim().toLowerCase() : "student";
				String userName = safeFirst + "_" + safeLast + generateRandomAlphaNumeric(3);
				String userCode = userName + "_" + generateRandomAlphaNumeric(5);
				String rawPassword = generateRandomAlphaNumeric(6);
				String encodedPassword = passwordEncoder.encode(rawPassword);

				studentUser.setUserName(userName);
				studentUser.setUserCode(userCode);
				studentUser.setPassword(encodedPassword);
				studentUser.setRoles(List.of(userRole));

				studentUser = userRepository.save(studentUser);
			} catch (Exception e) {
				logger.error("Failed to save student user: {}", e.getMessage(), e);
				throw new RuntimeException("Failed to create student user. Please check the input.");
			}

			// --- Step 3: Create and Save Guardian User ---
			User guardianUser = new User();
			try {
				guardianUser.setFirstName(dto.getGuardianFirstName());
				guardianUser.setMiddleName(dto.getGuardianMiddleName());
				guardianUser.setLastName(dto.getGuardianLastName());
				guardianUser.setEmail(dto.getGuardianEmail());
				guardianUser.setMobileNumber(Long.parseLong(dto.getGuardianPhoneNumber()));
				guardianUser.setAddress(dto.getGuardianAddress());
				guardianUser.setState(dto.getPermanentState());
				guardianUser.setCity(dto.getPermanentCity());

				String safeFirst = dto.getGuardianFirstName() != null ? dto.getGuardianFirstName().trim().toLowerCase() : "guardian";
				String safeLast = dto.getGuardianLastName() != null ? dto.getGuardianLastName().trim().toLowerCase() : "user";
				String userName = safeFirst + "_" + safeLast + generateRandomAlphaNumeric(3);
				String userCode = userName + "_" + generateRandomAlphaNumeric(5);
				String rawPassword = generateRandomAlphaNumeric(6);
				String encodedPassword = passwordEncoder.encode(rawPassword);

				guardianUser.setUserName(userName);
				guardianUser.setUserCode(userCode);
				guardianUser.setPassword(encodedPassword);
				guardianUser.setRoles(List.of(userRole));

				guardianUser = userRepository.save(guardianUser);
			} catch (Exception e) {
				logger.error("Failed to save guardian user: {}", e.getMessage(), e);
				throw new RuntimeException("Failed to create guardian user. Please check the input.");
			}

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
			} catch (Exception e) {
				logger.error("Failed to save guardian entity: {}", e.getMessage(), e);
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
			//	student.setAge(Long.parseLong(dto.getAge()));
				student.setAge(10L);

				Address current = new Address();
				current.setCountry(dto.getCurrentCountry());
				current.setState(dto.getCurrentState());
				current.setCity(dto.getCurrentCity());
				current.setZipCode(dto.getCurrentZipCode());
				current.setAddressLine(dto.getCurrentAddressLine());
				System.out.println("IN dto same adress"+dto.getIsSameAddress());

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


					System.out.println("in document");
					for (int i = 0; i < documents.size(); i++) {
						MultipartFile doc = documents.get(i);
						String type = types.get(i);
						System.out.println("type"+i);
						saveFile(doc, "doc_" + type + "_" + dto.getEmail());
					}


			} catch (Exception e) {
				logger.error("Failed to save student entity: {}", e.getMessage(), e);
				throw new RuntimeException("Failed to save student information.");
			}

			return student;

		} catch (Exception e) {
			logger.error("Transaction failed: {}", e.getMessage(), e);
			throw e; // This will trigger rollback
		}
	}



	private String saveFile(MultipartFile file, String prefix) throws IOException {
		System.out.println("In save FIle");
		if (file == null || file.isEmpty()) return "File is null";

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

	private String generateRandomAlphaNumeric(int length) {
		String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
		StringBuilder result = new StringBuilder(length);
		ThreadLocalRandom random = ThreadLocalRandom.current();
		for (int i = 0; i < length; i++) {
			result.append(characters.charAt(random.nextInt(characters.length())));
		}
		return result.toString();
	}


}
