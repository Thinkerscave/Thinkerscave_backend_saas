package com.thinkerscave.academics.service;

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

import java.util.List;

public interface SyllabusService {

    // Syllabus
    SyllabusResponse createSyllabus(SyllabusRequest request);

    SyllabusResponse getSyllabusById(Long syllabusId);

    List<SyllabusResponse> getSyllabusByClassAndYear(Long classId, Long yearId);

    SyllabusResponse publishSyllabus(Long syllabusId);

    void deleteSyllabus(Long syllabusId);

    // Unit
    UnitResponse addUnit(Long syllabusId, UnitRequest request);

    UnitResponse updateUnit(Long unitId, UnitRequest request);

    List<UnitResponse> getUnits(Long syllabusId);

    void deleteUnit(Long unitId);

    // Chapter
    ChapterResponse addChapter(Long unitId, ChapterRequest request);

    ChapterResponse updateChapter(Long chapterId, ChapterRequest request);

    List<ChapterResponse> getChapters(Long unitId);

    void deleteChapter(Long chapterId);

    // Topic
    TopicResponse addTopic(Long chapterId, TopicRequest request);

    TopicResponse updateTopic(Long topicId, TopicRequest request);

    List<TopicResponse> getTopics(Long chapterId);

    void deleteTopic(Long topicId);

    // Progress
    TopicResponse updateTopicProgress(Long topicId, TopicProgressRequest request);

    SyllabusProgressResponse getSyllabusProgress(Long syllabusId, Long teacherId);
}
