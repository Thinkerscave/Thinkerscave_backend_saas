package com.thinkerscave.academics.dto.request;

import jakarta.validation.constraints.Size;

public record TeacherArrangementReviewRequest(@Size(max = 500) String note) {}