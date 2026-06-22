package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.dto.request.ChapterRequest;
import com.thinkerscave.academics.dto.request.SyllabusRequest;
import com.thinkerscave.academics.dto.request.TopicProgressRequest;
import com.thinkerscave.academics.dto.request.TopicRequest;
import com.thinkerscave.academics.dto.request.UnitRequest;
import com.thinkerscave.academics.dto.response.ChapterResponse;
import com.thinkerscave.academics.dto.response.SyllabusProgressResponse;
import com.thinkerscave.academics.dto.response.SyllabusResponse;
import com.thinkerscave.academics.dto.response.TopicResponse;
import com.thinkerscave.academics.dto.response.UnitResponse;
import com.thinkerscave.academics.entity.AcademicClass;
import com.thinkerscave.academics.entity.AcademicYear;
import com.thinkerscave.academics.entity.Chapter;
import com.thinkerscave.academics.entity.Subject;
import com.thinkerscave.academics.entity.Syllabus;
import com.thinkerscave.academics.entity.SyllabusCoverage;
import com.thinkerscave.academics.entity.Topic;
import com.thinkerscave.academics.entity.Unit;
import com.thinkerscave.academics.enums.CoverageStatus;
import com.thinkerscave.academics.repository.AcademicYearRepository;
import com.thinkerscave.academics.repository.ChapterRepository;
import com.thinkerscave.academics.repository.ClassRepository;
import com.thinkerscave.academics.repository.SubjectRepository;
import com.thinkerscave.academics.repository.SyllabusCoverageRepository;
import com.thinkerscave.academics.repository.SyllabusRepository;
import com.thinkerscave.academics.repository.TopicRepository;
import com.thinkerscave.academics.repository.UnitRepository;
import com.thinkerscave.academics.service.SyllabusService;
import com.thinkerscave.shared.exceptions.AlreadyExistsException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyllabusServiceImpl implements SyllabusService {

    private final SyllabusRepository syllabusRepository;
    private final UnitRepository unitRepository;
    private final ChapterRepository chapterRepository;
    private final TopicRepository topicRepository;
    private final SyllabusCoverageRepository coverageRepository;
    private final AcademicYearRepository academicYearRepository;
    private final ClassRepository classRepository;
    private final SubjectRepository subjectRepository;

    // ---- Syllabus ----

    @Override
    @Transactional
    public SyllabusResponse createSyllabus(SyllabusRequest request) {
        if (syllabusRepository.findByAcademicYear_AcademicYearIdAndAcademicClass_ClassIdAndSubject_SubjectIdAndActiveTrue(
                request.getAcademicYearId(), request.getClassId(), request.getSubjectId()).isPresent()) {
            throw new AlreadyExistsException("Syllabus already exists for this year, class and subject");
        }
        AcademicYear year = getYear(request.getAcademicYearId());
        AcademicClass cls = getClass(request.getClassId());
        Subject subject = getSubject(request.getSubjectId());

        Syllabus syllabus = new Syllabus();
        syllabus.setAcademicYear(year);
        syllabus.setAcademicClass(cls);
        syllabus.setSubject(subject);
        syllabus.setTitle(request.getTitle());
        syllabus.setRemarks(request.getRemarks());
        syllabus.setPublished(false);
        syllabus.setActive(true);
        return toSyllabusResponse(syllabusRepository.save(syllabus), false);
    }

    @Override
    public SyllabusResponse getSyllabusById(Long syllabusId) {
        return toSyllabusResponse(getSyllabus(syllabusId), true);
    }

    @Override
    public List<SyllabusResponse> getSyllabusByClassAndYear(Long classId, Long yearId) {
        return syllabusRepository.findByAcademicYear_AcademicYearIdAndAcademicClass_ClassIdAndActiveOrderByTitleAsc(yearId, classId, true)
                .stream().map(s -> toSyllabusResponse(s, false)).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SyllabusResponse publishSyllabus(Long syllabusId) {
        Syllabus syllabus = getSyllabus(syllabusId);
        syllabus.setPublished(true);
        return toSyllabusResponse(syllabusRepository.save(syllabus), false);
    }

    @Override
    @Transactional
    public void deleteSyllabus(Long syllabusId) {
        Syllabus syllabus = getSyllabus(syllabusId);
        syllabus.setActive(false);
        syllabusRepository.save(syllabus);
    }

    // ---- Unit ----

    @Override
    @Transactional
    public UnitResponse addUnit(Long syllabusId, UnitRequest request) {
        Syllabus syllabus = getSyllabus(syllabusId);
        Unit unit = new Unit();
        unit.setSyllabus(syllabus);
        unit.setUnitNumber(request.getUnitNumber());
        unit.setUnitName(request.getUnitName());
        unit.setEstimatedHours(request.getEstimatedHours());
        unit.setDisplayOrder(request.getDisplayOrder());
        unit.setRemarks(request.getRemarks());
        unit.setActive(true);
        return toUnitResponse(unitRepository.save(unit), false);
    }

    @Override
    @Transactional
    public UnitResponse updateUnit(Long unitId, UnitRequest request) {
        Unit unit = getUnit(unitId);
        unit.setUnitNumber(request.getUnitNumber());
        unit.setUnitName(request.getUnitName());
        unit.setEstimatedHours(request.getEstimatedHours());
        unit.setDisplayOrder(request.getDisplayOrder());
        unit.setRemarks(request.getRemarks());
        return toUnitResponse(unitRepository.save(unit), false);
    }

    @Override
    public List<UnitResponse> getUnits(Long syllabusId) {
        return unitRepository.findBySyllabus_SyllabusIdAndActiveOrderByUnitNumberAsc(syllabusId, true)
                .stream().map(u -> toUnitResponse(u, true)).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteUnit(Long unitId) {
        Unit unit = getUnit(unitId);
        unit.setActive(false);
        unitRepository.save(unit);
    }

    // ---- Chapter ----

    @Override
    @Transactional
    public ChapterResponse addChapter(Long unitId, ChapterRequest request) {
        Unit unit = getUnit(unitId);
        Chapter chapter = new Chapter();
        chapter.setUnit(unit);
        chapter.setChapterNumber(request.getChapterNumber());
        chapter.setChapterName(request.getChapterName());
        chapter.setEstimatedHours(request.getEstimatedHours());
        chapter.setDisplayOrder(request.getDisplayOrder());
        chapter.setRemarks(request.getRemarks());
        chapter.setActive(true);
        return toChapterResponse(chapterRepository.save(chapter), false);
    }

    @Override
    @Transactional
    public ChapterResponse updateChapter(Long chapterId, ChapterRequest request) {
        Chapter chapter = getChapter(chapterId);
        chapter.setChapterNumber(request.getChapterNumber());
        chapter.setChapterName(request.getChapterName());
        chapter.setEstimatedHours(request.getEstimatedHours());
        chapter.setDisplayOrder(request.getDisplayOrder());
        chapter.setRemarks(request.getRemarks());
        return toChapterResponse(chapterRepository.save(chapter), false);
    }

    @Override
    public List<ChapterResponse> getChapters(Long unitId) {
        return chapterRepository.findByUnit_UnitIdAndActiveOrderByChapterNumberAsc(unitId, true)
                .stream().map(c -> toChapterResponse(c, true)).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteChapter(Long chapterId) {
        Chapter chapter = getChapter(chapterId);
        chapter.setActive(false);
        chapterRepository.save(chapter);
    }

    // ---- Topic ----

    @Override
    @Transactional
    public TopicResponse addTopic(Long chapterId, TopicRequest request) {
        Chapter chapter = getChapter(chapterId);
        Topic topic = new Topic();
        topic.setChapter(chapter);
        topic.setTopicNumber(request.getTopicNumber());
        topic.setTopicName(request.getTopicName());
        topic.setEstimatedHours(request.getEstimatedHours());
        topic.setDisplayOrder(request.getDisplayOrder());
        topic.setRemarks(request.getRemarks());
        topic.setActive(true);
        return toTopicResponse(topicRepository.save(topic), null);
    }

    @Override
    @Transactional
    public TopicResponse updateTopic(Long topicId, TopicRequest request) {
        Topic topic = getTopic(topicId);
        topic.setTopicNumber(request.getTopicNumber());
        topic.setTopicName(request.getTopicName());
        topic.setEstimatedHours(request.getEstimatedHours());
        topic.setDisplayOrder(request.getDisplayOrder());
        topic.setRemarks(request.getRemarks());
        return toTopicResponse(topicRepository.save(topic), null);
    }

    @Override
    public List<TopicResponse> getTopics(Long chapterId) {
        return topicRepository.findByChapter_ChapterIdAndActiveOrderByTopicNumberAsc(chapterId, true)
                .stream().map(t -> toTopicResponse(t, null)).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteTopic(Long topicId) {
        Topic topic = getTopic(topicId);
        topic.setActive(false);
        topicRepository.save(topic);
    }

    // ---- Progress ----

    @Override
    @Transactional
    public TopicResponse updateTopicProgress(Long topicId, TopicProgressRequest request) {
        Topic topic = getTopic(topicId);
        CoverageStatus status = CoverageStatus.valueOf(request.getStatus());

        SyllabusCoverage coverage = coverageRepository.findByTopic_TopicId(topicId).orElse(null);
        if (coverage == null || !coverage.getTeacherId().equals(request.getTeacherId())) {
            coverage = new SyllabusCoverage();
            coverage.setTopic(topic);
            coverage.setTeacherId(request.getTeacherId());
        }
        coverage.setStatus(status);
        coverage.setCompletionDate(request.getCompletionDate());
        coverage.setRemarks(request.getRemarks());
        coverageRepository.save(coverage);

        return toTopicResponse(topic, status.name());
    }

    @Override
    public SyllabusProgressResponse getSyllabusProgress(Long syllabusId, Long teacherId) {
        List<Topic> topics = topicRepository.findByChapter_Unit_Syllabus_SyllabusIdAndActiveTrue(syllabusId);
        int total = topics.size();
        int completed = 0;
        int inProgress = 0;
        int notStarted = 0;

        for (Topic t : topics) {
            String statusStr = coverageRepository.findByTopic_TopicId(t.getTopicId())
                    .filter(c -> teacherId == null || c.getTeacherId().equals(teacherId))
                    .map(c -> c.getStatus().name())
                    .orElse(CoverageStatus.NOT_STARTED.name());
            if (CoverageStatus.COMPLETED.name().equals(statusStr)) completed++;
            else if (CoverageStatus.IN_PROGRESS.name().equals(statusStr)) inProgress++;
            else notStarted++;
        }

        double pct = total > 0 ? Math.round((completed * 100.0 / total) * 10.0) / 10.0 : 0.0;
        return SyllabusProgressResponse.builder()
                .syllabusId(syllabusId)
                .totalTopics(total)
                .completedTopics(completed)
                .inProgressTopics(inProgress)
                .notStartedTopics(notStarted)
                .completionPercentage(pct)
                .build();
    }

    // ---- helpers ----

    private AcademicYear getYear(Long id) {
        return academicYearRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found: " + id));
    }

    private AcademicClass getClass(Long id) {
        return classRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found: " + id));
    }

    private Subject getSubject(Long id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + id));
    }

    private Syllabus getSyllabus(Long id) {
        return syllabusRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Syllabus not found: " + id));
    }

    private Unit getUnit(Long id) {
        return unitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unit not found: " + id));
    }

    private Chapter getChapter(Long id) {
        return chapterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter not found: " + id));
    }

    private Topic getTopic(Long id) {
        return topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found: " + id));
    }

    private SyllabusResponse toSyllabusResponse(Syllabus s, boolean includeUnits) {
        List<UnitResponse> units = includeUnits
                ? unitRepository.findBySyllabus_SyllabusIdAndActiveOrderByUnitNumberAsc(s.getSyllabusId(), true)
                .stream().map(u -> toUnitResponse(u, true)).collect(Collectors.toList())
                : null;
        return SyllabusResponse.builder()
                .syllabusId(s.getSyllabusId())
                .academicYearId(s.getAcademicYear().getAcademicYearId())
                .classId(s.getAcademicClass().getClassId())
                .className(s.getAcademicClass().getClassName())
                .subjectId(s.getSubject().getSubjectId())
                .subjectName(s.getSubject().getSubjectName())
                .title(s.getTitle())
                .versionNo(s.getVersionNo())
                .published(s.getPublished())
                .active(s.getActive())
                .units(units)
                .build();
    }

    private UnitResponse toUnitResponse(Unit u, boolean includeChapters) {
        List<ChapterResponse> chapters = includeChapters
                ? chapterRepository.findByUnit_UnitIdAndActiveOrderByChapterNumberAsc(u.getUnitId(), true)
                .stream().map(c -> toChapterResponse(c, true)).collect(Collectors.toList())
                : null;
        return UnitResponse.builder()
                .unitId(u.getUnitId())
                .unitNumber(u.getUnitNumber())
                .unitName(u.getUnitName())
                .estimatedHours(u.getEstimatedHours())
                .displayOrder(u.getDisplayOrder())
                .active(u.getActive())
                .chapters(chapters)
                .build();
    }

    private ChapterResponse toChapterResponse(Chapter c, boolean includeTopics) {
        List<TopicResponse> topics = includeTopics
                ? topicRepository.findByChapter_ChapterIdAndActiveOrderByTopicNumberAsc(c.getChapterId(), true)
                .stream().map(t -> toTopicResponse(t, null)).collect(Collectors.toList())
                : null;
        return ChapterResponse.builder()
                .chapterId(c.getChapterId())
                .chapterNumber(c.getChapterNumber())
                .chapterName(c.getChapterName())
                .estimatedHours(c.getEstimatedHours())
                .displayOrder(c.getDisplayOrder())
                .active(c.getActive())
                .topics(topics)
                .build();
    }

    private TopicResponse toTopicResponse(Topic t, String coverageStatus) {
        return TopicResponse.builder()
                .topicId(t.getTopicId())
                .topicNumber(t.getTopicNumber())
                .topicName(t.getTopicName())
                .estimatedHours(t.getEstimatedHours())
                .displayOrder(t.getDisplayOrder())
                .active(t.getActive())
                .coverageStatus(coverageStatus)
                .build();
    }
}
