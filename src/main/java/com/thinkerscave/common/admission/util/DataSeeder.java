package com.thinkerscave.common.admission.util;

import com.thinkerscave.common.admission.domain.FollowUp;
import com.thinkerscave.common.admission.domain.Inquiry;
import com.thinkerscave.common.admission.enums.FollowUpType;
import com.thinkerscave.common.admission.enums.InquiryStatus;
import com.thinkerscave.common.admission.repository.FollowUpRepository;
import com.thinkerscave.common.admission.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final InquiryRepository inquiryRepository;
    private final FollowUpRepository followUpRepository;

    @Override
    public void run(String... args) throws Exception {
        if (inquiryRepository.count() > 15) {
            return;
        }

        log.info("Seeding Inquiry Data...");

        createInquiry("Rohan Sharma", "9876543210", "rohan@example.com", "Class 10", "WEBSITE", InquiryStatus.CONTACTED,
                LocalDate.now(), "Call", true);
        createInquiry("Priya Patel", "9123456789", "priya@example.com", "Class 8", "WALK_IN",
                InquiryStatus.FOLLOW_UP_REQUIRED, LocalDate.now().minusDays(2), "WhatsApp", true); // Overdue
        createInquiry("Amit Kumar", "8765432109", "amit@example.com", "Class 12", "REFERRAL",
                InquiryStatus.FOLLOW_UP_REQUIRED, LocalDate.now().plusDays(2), "Call", true); // Upcoming
        createInquiry("Sneha Reddy", "7654321098", "sneha@example.com", "Class 5", "SOCIAL_MEDIA",
                InquiryStatus.CONVERTED, null, "Call", true); // Converted
        createInquiry("Vikash Singh", "9988776655", "vikash@example.com", "Class 10", "PHONE", InquiryStatus.NEW,
                LocalDate.now().plusDays(5), null, false); // New
        createInquiry("Meera Nair", "8877665544", "meera@example.com", "Class 9", "AD", InquiryStatus.LOST, null,
                "Email", true); // Lost

        // Add more random data to reach ~15
        createInquiry("John Doe", "1231231234", "john@test.com", "Class 11", "WEBSITE", InquiryStatus.NEW,
                LocalDate.now(), null, false);
        createInquiry("Jane Smith", "3213214321", "jane@test.com", "Class 7", "WALK_IN", InquiryStatus.CONTACTED,
                LocalDate.now().plusDays(1), "Call", true);
        createInquiry("Robert Wilson", "5556667777", "rob@test.com", "Class 6", "PHONE",
                InquiryStatus.FOLLOW_UP_REQUIRED, LocalDate.now().minusDays(5), "SMS", true); // Overdue
        createInquiry("Emily Davis", "9998887777", "emily@test.com", "Class 8", "WEBSITE",
                InquiryStatus.READY_FOR_ADMISSION, null, "WhatsApp", true); // Ready
        createInquiry("Michael Brown", "1112223333", "mike@test.com", "Class 12", "REFERRAL", InquiryStatus.CONTACTED,
                LocalDate.now(), "WhatsApp", true); // Today
        createInquiry("Sarah Miller", "4445556666", "sarah@test.com", "Class 10", "SOCIAL_MEDIA", InquiryStatus.NEW,
                LocalDate.now().plusDays(3), null, false);
        createInquiry("David Garcia", "7778889999", "david@test.com", "Class 9", "WEBSITE",
                InquiryStatus.FOLLOW_UP_REQUIRED, LocalDate.now().minusDays(1), "Call", true); // Overdue
        createInquiry("Jennifer Martinez", "2223334444", "jen@test.com", "Class 11", "WALK_IN", InquiryStatus.CONTACTED,
                LocalDate.now().plusDays(10), "Email", true); // Upcoming
        createInquiry("William Anderson", "6667778888", "will@test.com", "Class 7", "PHONE", InquiryStatus.NEW,
                LocalDate.now(), null, false); // Today New

        log.info("Data Seeding Completed.");
    }

    private void createInquiry(String name, String mobile, String email, String clazz, String source,
            InquiryStatus status, LocalDate nextDate, String lastType, boolean addFollowUp) {
        Inquiry inquiry = Inquiry.builder()
                .name(name)
                .mobileNumber(mobile)
                .email(email)
                .classInterestedIn(clazz)
                .inquirySource(source)
                .assignedCounselorId(1L) // Assign to ID 1
                .status(status)
                .nextFollowUpDate(nextDate)
                .build();

        inquiry.setCreatedDate(new Date());
        inquiry.setCreatedBy("admin");

        if (lastType != null) {
            inquiry.setLastFollowUpType(FollowUpType.valueOf(lastType.toUpperCase()));
            inquiry.setLastFollowUpDate(LocalDateTime.now().minusDays(1));
        }

        Inquiry saved = inquiryRepository.save(inquiry);

        if (addFollowUp && lastType != null) {
            FollowUp followUp = FollowUp.builder()
                    .inquiry(saved)
                    .followUpType(FollowUpType.valueOf(lastType.toUpperCase()))
                    .remarks("Auto-generated follow-up")
                    .statusAfterFollowUp(status)
                    .followUpDate(LocalDateTime.now().minusDays(1))
                    .nextFollowUpDate(nextDate)
                    .build();
            followUp.setCreatedBy("admin");
            followUpRepository.save(followUp);
        }
    }
}
