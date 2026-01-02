package com.thinkerscave.common.course.service.impl;

import com.thinkerscave.common.course.domain.Chapter;
import com.thinkerscave.common.course.domain.Subject;
import com.thinkerscave.common.course.domain.Syllabus;
import com.thinkerscave.common.course.domain.Topic;
import com.thinkerscave.common.course.dto.ChapterDTO;
import com.thinkerscave.common.course.dto.SyllabusRequestDTO;
import com.thinkerscave.common.course.dto.SyllabusResponseDTO;
import com.thinkerscave.common.course.dto.TopicDTO;
import com.thinkerscave.common.course.enums.SyllabusStatus;
import com.thinkerscave.common.course.repository.ChapterRepository;
import com.thinkerscave.common.course.repository.SubjectRepository;
import com.thinkerscave.common.course.repository.SyllabusRepository;
import com.thinkerscave.common.course.repository.TopicRepository;
import com.thinkerscave.common.course.service.SyllabusService;
import com.thinkerscave.common.exception.ResourceNotFoundException;
import com.thinkerscave.common.usrm.domain.User;
import com.thinkerscave.common.usrm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 🎓 SyllabusServiceImpl - The Core Curriculum Engine of ThinkersCave
 * 
 * 🏛️ Business Purpose:
 * This service implementation serves as the primary engine for managing the
 * educational
 * blueprints (Syllabi) across the ThinkersCave SaaS platform. It handles the
 * complex
 * hierarchical structure of academic content—breaking down a subject into
 * modular
 * Chapters, and further into granular, teachable Topics.
 * 
 * 👥 User Roles & Stakeholders:
 * - **Administrators / HODs**: Trigger creation, approval, and versioning
 * workflows
 * to maintain institutional standards and quality control.
 * - **Teachers**: Interface with this service (via UI) to roadmap their
 * teaching
 * schedules and track the progress of their delivery.
 * - **Students**: The end consumers of this data; they rely on the output of
 * this
 * service to understand their learning journey and track their own progress.
 * - **System Modules**: Referenced by the Gradebook, Attendance, and Progress
 * Reporting modules for data consistency.
 * 
 * 🔄 Academic Flow Position:
 * This class operates in the "Curriculum Planning & Delivery" phase. It sits
 * between the high-level Academic Structure (Schools/Courses) and the low-level
 * student interactions (Progress Tracking).
 * 
 * 🏗️ Design Intent:
 * Built with a "Draft-First" and "Strict Mutability" philosophy. It ensures
 * that once a curriculum is approved or published, it cannot be accidentally
 * altered—preventing data corruption for historical student progress reports.
 * 
 * 🚀 Future Extensibility:
 * - Integration with Learning Management Systems (LMS) for automated resource
 * linking.
 * - Support for AI-driven lesson planning based on these syllabus structures.
 * - Cross-tenant curriculum sharing (Marketplace features).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SyllabusServiceImpl implements SyllabusService {

    /**
     * Primary repository for Syllabus header data.
     * Used to manage the root curriculum entities and their status transitions.
     */
    private final SyllabusRepository syllabusRepository;

    /**
     * Repository for Academic Subjects.
     * Essential for linking a syllabus to the correct educational context (e.g.,
     * "Mathematics 101").
     */
    private final SubjectRepository subjectRepository;

    /**
     * Repository for Chapter-level content.
     * Manages the first level of the content hierarchy within a syllabus.
     */
    private final ChapterRepository chapterRepository;

    /**
     * Repository for individual Topics.
     * Handles the most granular level of data, essential for student-level progress
     * tracking.
     */
    private final TopicRepository topicRepository;

    /**
     * Repository for User management.
     * Specifically used in the approval workflow to log and authorize the person
     * validating the curriculum.
     */
    private final UserRepository userRepository;

    /**
     * 🆕 createSyllabus
     * 
     * 🛠️ Purpose: Initializes a new syllabus entry in the system.
     * ⏰ When it is called: Triggered by a Teacher or Admin when a new subject is
     * introduced or a new lesson plan is started.
     * 👤 Triggered by: UI interaction (Syllabus Designer).
     * 
     * @param dto Data Transfer Object containing the initial title, description,
     *            and content hierarchy.
     * @return SyllabusResponseDTO A shallow or deep representation of the newly
     *         created draft.
     * 
     *         ⚠️ Side Effects: Inserts records into 'syllabus', 'chapters', and
     *         'topics' tables.
     */
    @Override
    @Transactional
    public SyllabusResponseDTO createSyllabus(SyllabusRequestDTO dto) {
        // Business Rule: A syllabus cannot exist without a valid subject reference.
        // We validate this first to fail-fast and preserve data integrity.
        log.debug("Starting syllabus creation for Subject ID: {}", dto.getSubjectId());
        Subject subject = subjectRepository.findById(dto.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));

        Syllabus syllabus = new Syllabus();

        // Logical check: If the UI doesn't provide a human-readable code, we generate
        // a stable internal UUID-based code for system identification.
        syllabus.setSyllabusCode(dto.getSyllabusCode() != null ? dto.getSyllabusCode() : generateCode("SYL"));

        syllabus.setTitle(dto.getTitle());
        syllabus.setDescription(dto.getDescription());

        // Versioning Logic: Defaults to 1.0 for new creations to start a clean version
        // chain.
        syllabus.setVersion(dto.getVersion() != null ? dto.getVersion() : "1.0");
        syllabus.setSubject(subject);

        // Security/Workflow: New syllabi are ALWAYS DRAFT to ensure mandatory
        // internal review before they become visible to students.
        syllabus.setStatus(SyllabusStatus.DRAFT);
        syllabus.setIsActive(true);

        Syllabus saved = syllabusRepository.save(syllabus);

        // Recursive persistence mapping: Since a syllabus is a complex tree, we
        // handle the nested Chapter/Topic persistence here.
        if (dto.getChapters() != null) {
            saveChaptersAndTopics(saved, dto.getChapters());
        }

        // We return the full representation (re-fetched) to ensure the UI has the
        // generated IDs.
        return getSyllabus(saved.getSyllabusId());
    }

    /**
     * 📝 updateSyllabus
     * 
     * 🛠️ Purpose: Modifies the metadata or content of an existing syllabus.
     * ⏰ When it is called: During the planning phase before approval.
     * 👤 Triggered by: The original creator or an authorized editor.
     * 
     * @param syllabusId The ID of the syllabus to modify.
     * @param dto        The new content and metadata.
     * @return SyllabusResponseDTO The updated state of the syllabus.
     * 
     *         🔒 Business Rule: Strict mutability—Syllabi that are Approved or
     *         Published
     *         are locked. This prevents data loss for students currently being
     *         graded!
     */
    @Override
    @Transactional
    public SyllabusResponseDTO updateSyllabus(Long syllabusId, SyllabusRequestDTO dto) {
        Syllabus syllabus = syllabusRepository.findById(syllabusId)
                .orElseThrow(() -> new ResourceNotFoundException("Syllabus not found"));

        // Safeguard Migration Check:
        // We must ensure the syllabus is still in DRAFT. If it's already approved,
        // the user MUST create a new version instead of overwriting this one.
        if (syllabus.getStatus() != SyllabusStatus.DRAFT) {
            log.error("Attempted to update non-draft syllabus ID: {}. Current Status: {}",
                    syllabusId, syllabus.getStatus());
            throw new IllegalStateException("Only draft syllabi can be updated. Create a new version instead.");
        }

        syllabus.setTitle(dto.getTitle());
        syllabus.setDescription(dto.getDescription());
        syllabus.setVersion(dto.getVersion());

        // Hierarchy Refresh Strategy:
        // For simplicity and to avoid complex 'dirty check' logic for nested lists,
        // we wipe existing chapters and re-insert the provided ones.
        // This ensures the DB exactly matches the UI state after the update.
        chapterRepository.deleteAll(syllabus.getChapters());
        syllabus.getChapters().clear();

        if (dto.getChapters() != null) {
            saveChaptersAndTopics(syllabus, dto.getChapters());
        }

        Syllabus saved = syllabusRepository.save(syllabus);
        return getSyllabus(saved.getSyllabusId());
    }

    /**
     * 🔍 getSyllabus
     * 
     * 🛠️ Purpose: Retrieves the full content of a specific syllabus.
     * ⏰ When it is called: When a user clicks into a specific curriculum version.
     * 
     * @param syllabusId Unique ID of the syllabus.
     * @return SyllabusResponseDTO Deep object including chapters and topics.
     */
    @Override
    public SyllabusResponseDTO getSyllabus(Long syllabusId) {
        log.info("Fetching syllabus details for ID: {}", syllabusId);
        Syllabus syllabus = syllabusRepository.findById(syllabusId)
                .orElseThrow(() -> new ResourceNotFoundException("Syllabus not found"));
        return mapToResponseDTO(syllabus);
    }

    /**
     * 📅 getLatestSyllabusBySubject
     * 
     * 🛠️ Purpose: Finds the most relevant curriculum version for a subject.
     * ⏰ When it is called: Primarily by the Student Dashboard to show "What to
     * learn today".
     * 
     * @param subjectId The subject context.
     * @return SyllabusResponseDTO The latest version found.
     */
    @Override
    public SyllabusResponseDTO getLatestSyllabusBySubject(Long subjectId) {
        // Business Rationale: A subject might have 5 versions of a syllabus over
        // 5 years. This logic ensures we pick the one that is current (highest
        // version).
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));

        Syllabus syllabus = syllabusRepository.findBySubjectOrderByVersionDesc(subject)
                .stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No syllabus found for this subject"));

        return mapToResponseDTO(syllabus);
    }

    /**
     * 📋 getAllSyllabiBySubject
     * 
     * 🛠️ Purpose: Shows the history of all curricula for a subject.
     * 
     * @param subjectId The subject context.
     * @return List<SyllabusResponseDTO> version history.
     */
    @Override
    public List<SyllabusResponseDTO> getAllSyllabiBySubject(Long subjectId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        return syllabusRepository.findBySubject(subject)
                .stream().map(this::mapToResponseDTO).collect(Collectors.toList());
    }

    /**
     * ✅ approveSyllabus
     * 
     * 🛠️ Purpose: Transitions a syllabus from Planning to Validated state.
     * ⏰ When it is called: When an HOD or Admin reviews and clears a Draft.
     * 👤 Triggered by: Admin UI.
     * 
     * @param syllabusId The syllabus to validate.
     * @param userId     The ID of the approver (for audit trail).
     */
    @Override
    @Transactional
    public void approveSyllabus(Long syllabusId, Long userId) {
        Syllabus syllabus = syllabusRepository.findById(syllabusId)
                .orElseThrow(() -> new ResourceNotFoundException("Syllabus not found"));

        // Verification: We must know WHO approved this syllabus for compliance and
        // accreditation reports (ISO/Academic Standards).
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        syllabus.setStatus(SyllabusStatus.APPROVED);
        syllabus.setApprovedBy(user);
        syllabus.setApprovedDate(LocalDate.now());

        log.info("Syllabus ID: {} successfully APPROVED by user: {}", syllabusId, user.getEmail());
        syllabusRepository.save(syllabus);
    }

    /**
     * 🌍 publishSyllabus
     * 
     * 🛠️ Purpose: Makes the curriculum visible to the entire school/institution.
     * ⏰ When it is called: After final approval, usually at the start of a
     * semester.
     * 
     * @param syllabusId The syllabus to publish.
     */
    @Override
    @Transactional
    public void publishSyllabus(Long syllabusId) {
        Syllabus syllabus = syllabusRepository.findById(syllabusId)
                .orElseThrow(() -> new ResourceNotFoundException("Syllabus not found"));

        // Rule: Prevent direct publishing of drafts. Everything must be
        // reviewed (APPROVED) at least once.
        if (syllabus.getStatus() != SyllabusStatus.APPROVED) {
            log.warn("Attempted to PUBLISH unapproved syllabus ID: {}", syllabusId);
            throw new IllegalStateException("Syllabus must be approved before publishing");
        }

        syllabus.setStatus(SyllabusStatus.PUBLISHED);
        syllabus.setPublishedDate(LocalDate.now());
        syllabusRepository.save(syllabus);
    }

    /**
     * 🧬 createNewVersion (The Content Cloner)
     * 
     * 🛠️ Purpose: Deep-copies an entire curriculum hierarchy into a new version.
     * 🏛️ Business Rationale: In many educational models, the syllabus for 2024
     * is 95% identical to 2023. Manual re-entry is a massive pain point.
     * This method provides "One-Click Migration".
     * 
     * @param oldSyllabusId The source curriculum.
     * @param newVersion    The name/number of the new version (e.g., "v2.0").
     * @return SyllabusResponseDTO The new draft clone.
     */
    @Override
    @Transactional
    public SyllabusResponseDTO createNewVersion(Long oldSyllabusId, String newVersion) {
        Syllabus oldSyllabus = syllabusRepository.findById(oldSyllabusId)
                .orElseThrow(() -> new ResourceNotFoundException("Old syllabus not found"));

        // Header Mapping: Cloning root properties
        Syllabus newSyllabus = new Syllabus();
        newSyllabus.setSyllabusCode(generateCode("SYL"));
        newSyllabus.setTitle(oldSyllabus.getTitle());
        newSyllabus.setDescription(oldSyllabus.getDescription());
        newSyllabus.setVersion(newVersion);
        newSyllabus.setSubject(oldSyllabus.getSubject());
        newSyllabus.setStatus(SyllabusStatus.DRAFT); // Clones are ALWAYS drafts initially
        newSyllabus.setPreviousVersion(oldSyllabus); // Link the history chain
        newSyllabus.setIsActive(true);

        Syllabus saved = syllabusRepository.save(newSyllabus);

        // Core Hierarchy Logic: Deep Cloning
        // We iterate through every chapter and topic to ensure a completely
        // independent copy is made, allowing the user to edit the clone
        // without affecting the original.
        for (Chapter oldChapter : oldSyllabus.getChapters()) {
            Chapter newChapter = new Chapter();
            newChapter.setChapterNumber(oldChapter.getChapterNumber());
            newChapter.setChapterName(oldChapter.getChapterName());
            newChapter.setDescription(oldChapter.getDescription());
            newChapter.setLearningObjectives(oldChapter.getLearningObjectives());
            newChapter.setSyllabus(saved);
            Chapter savedChapter = chapterRepository.save(newChapter);

            // Nested Loop: Cloning the third level of the hierarchy
            for (Topic oldTopic : oldChapter.getTopics()) {
                Topic newTopic = new Topic();
                newTopic.setTopicNumber(oldTopic.getTopicNumber());
                newTopic.setTopicName(oldTopic.getTopicName());
                newTopic.setDescription(oldTopic.getDescription());
                newTopic.setEstimatedHours(oldTopic.getEstimatedHours());
                newTopic.setChapter(savedChapter);
                topicRepository.save(newTopic);
            }
        }

        return getSyllabus(saved.getSyllabusId());
    }

    /**
     * 🗑️ deleteSyllabus
     * 
     * 🛠️ Purpose: Deactivates a curriculum.
     * 
     * @param syllabusId ID to remove.
     */
    @Override
    @Transactional
    public void deleteSyllabus(Long syllabusId) {
        Syllabus syllabus = syllabusRepository.findById(syllabusId)
                .orElseThrow(() -> new ResourceNotFoundException("Syllabus not found"));

        // Strategy: Soft Delete.
        // Why: Total deletion could break existing progress reports for
        // students who were previously enrolled against this syllabus.
        syllabus.setIsActive(false);
        syllabusRepository.save(syllabus);
    }

    /**
     * 📥 saveChaptersAndTopics (Internal Recursive Mapper)
     * 
     * 🏛️ Purpose: Manages the heavy lifting of converting DTO lists into
     * persisted database entities for the hierarchical syllabus structure.
     * 
     * @param syllabus    The parent syllabus entity.
     * @param chapterDTOs List of modules to persist.
     */
    private void saveChaptersAndTopics(Syllabus syllabus, List<ChapterDTO> chapterDTOs) {
        // Step 1: Process Chapters
        for (ChapterDTO cDto : chapterDTOs) {
            Chapter chapter = new Chapter();
            chapter.setChapterNumber(cDto.getChapterNumber());
            chapter.setChapterName(cDto.getChapterName());
            chapter.setDescription(cDto.getDescription());
            chapter.setLearningObjectives(cDto.getLearningObjectives());
            chapter.setSyllabus(syllabus);
            Chapter savedChapter = chapterRepository.save(chapter);

            // Step 2: Process individual Topics within each Chapter
            if (cDto.getTopics() != null) {
                for (TopicDTO tDto : cDto.getTopics()) {
                    Topic topic = new Topic();
                    topic.setTopicNumber(tDto.getTopicNumber());
                    topic.setTopicName(tDto.getTopicName());
                    topic.setDescription(tDto.getDescription());
                    topic.setEstimatedHours(tDto.getEstimatedHours());
                    topic.setChapter(savedChapter);
                    topicRepository.save(topic);
                }
            }
        }
    }

    /**
     * Maps the internal JPA Entity to a safe DTO for API transfer.
     */
    private SyllabusResponseDTO mapToResponseDTO(Syllabus syllabus) {
        return SyllabusResponseDTO.builder()
                .syllabusId(syllabus.getSyllabusId())
                .syllabusCode(syllabus.getSyllabusCode())
                .title(syllabus.getTitle())
                .description(syllabus.getDescription())
                .version(syllabus.getVersion())
                .status(syllabus.getStatus())
                .subjectName(syllabus.getSubject().getSubjectName())
                .approvedDate(syllabus.getApprovedDate())
                .publishedDate(syllabus.getPublishedDate())
                // Ensure child lists are also mapped correctly
                .chapters(syllabus.getChapters().stream().map(this::mapToChapterDTO).collect(Collectors.toList()))
                .build();
    }

    /**
     * Maps Chapter entities to DTOs.
     */
    private ChapterDTO mapToChapterDTO(Chapter chapter) {
        return ChapterDTO.builder()
                .chapterNumber(chapter.getChapterNumber())
                .chapterName(chapter.getChapterName())
                .description(chapter.getDescription())
                .learningObjectives(chapter.getLearningObjectives())
                // Recurse into topics
                .topics(chapter.getTopics().stream().map(this::mapToTopicDTO).collect(Collectors.toList()))
                .build();
    }

    /**
     * Maps individual Topic entities to DTOs.
     */
    private TopicDTO mapToTopicDTO(Topic topic) {
        return TopicDTO.builder()
                .topicNumber(topic.getTopicNumber())
                .topicName(topic.getTopicName())
                .description(topic.getDescription())
                .estimatedHours(topic.getEstimatedHours())
                .build();
    }

    /**
     * Code Utility: Generates a stable unique identifier for a business entity.
     * Why not just ID: Database IDs can change during migration; business codes
     * should remain immutable for report stability.
     */
    private String generateCode(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
