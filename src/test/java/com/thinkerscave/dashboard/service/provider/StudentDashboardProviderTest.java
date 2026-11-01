package com.thinkerscave.dashboard.service.provider;

import com.thinkerscave.attendance.repository.StudentAttendanceRepository;
import com.thinkerscave.communication.repository.NoticeRepository;
import com.thinkerscave.dashboard.dto.response.WidgetDTO;
import com.thinkerscave.dashboard.dto.response.widgetdata.QuickActionItem;
import com.thinkerscave.dashboard.dto.response.widgetdata.QuickActionsData;
import com.thinkerscave.dashboard.service.DashboardTimetableHelper;
import com.thinkerscave.student.repository.StudentEnrollmentRepository;
import com.thinkerscave.student.repository.StudentRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StudentDashboardProviderTest {

    @Test
    void linksStudentTimetableShortcutToSelfServicePage() {
        DashboardTimetableHelper timetableHelper = mock(DashboardTimetableHelper.class);
        when(timetableHelper.todaySlotsForSection(null)).thenReturn(List.of());
        StudentDashboardProvider provider = new StudentDashboardProvider(
                mock(StudentRepository.class),
                mock(StudentEnrollmentRepository.class),
                mock(StudentAttendanceRepository.class),
                mock(NoticeRepository.class),
                timetableHelper);

        WidgetDTO<?> quickActionsWidget = provider.getWidgets(null).stream()
                .filter(widget -> "quick-actions".equals(widget.getWidgetKey()))
                .findFirst()
                .orElseThrow();
        QuickActionsData quickActions = (QuickActionsData) quickActionsWidget.getData();
        QuickActionItem timetableLink = quickActions.getItems().stream()
                .filter(item -> "My Timetable".equals(item.getLabel()))
                .findFirst()
                .orElseThrow();
        QuickActionItem academicsLink = quickActions.getItems().stream()
                .filter(item -> "My Academics".equals(item.getLabel()))
                .findFirst()
                .orElseThrow();

        assertEquals("/app/academics/my-timetable", timetableLink.getRoute());
        assertEquals("/app/academics/my-academics", academicsLink.getRoute());
    }
}
