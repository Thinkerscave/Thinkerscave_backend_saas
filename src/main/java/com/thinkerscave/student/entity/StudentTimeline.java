package com.thinkerscave.student.entity;

import com.thinkerscave.shared.entity.Auditable;
import com.thinkerscave.student.enums.StudentTimelineEventType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "student_timeline")
public class StudentTimeline extends Auditable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "timeline_id")
	private Long timelineId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "student_id", nullable = false)
	private Student student;

	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", nullable = false)
	private StudentTimelineEventType eventType;

	@Column(name = "title", nullable = false)
	private String title;

	@Column(name = "description", columnDefinition = "TEXT")
	private String description;
}
