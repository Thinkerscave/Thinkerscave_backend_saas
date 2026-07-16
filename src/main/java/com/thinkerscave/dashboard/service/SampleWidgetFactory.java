package com.thinkerscave.dashboard.service;

import com.thinkerscave.dashboard.dto.response.widgetdata.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds realistic, clearly-{@code SAMPLE}-tagged payloads for domains that
 * have no backend module yet (Fee, Leave, Examination, Library, Transport,
 * Support Tickets). These are consumed through the exact same
 * {@code WidgetDTO} contract as live widgets so swapping to real data later
 * is a provider-level change only — no frontend rewrite.
 */
@Component
public class SampleWidgetFactory {

    public FeeSummaryData feeSummary() {
        return FeeSummaryData.builder()
                .totalDue(48500)
                .totalPaid(36000)
                .pendingAmount(12500)
                .nextDueDate(LocalDate.now().plusDays(9))
                .pendingInvoices(2)
                .currency("INR")
                .build();
    }

    public LeaveSummaryData leaveSummary() {
        return LeaveSummaryData.builder()
                .availableDays(14)
                .usedDays(6)
                .pendingRequests(1)
                .lastRequestStatus("Approved")
                .build();
    }

    public ExaminationSummaryData examinationSummary() {
        return ExaminationSummaryData.builder()
                .upcoming(List.of(
                        StatListItem.builder().label("Mid-Term — Mathematics").value(dateLabel(6)).icon("pi-pencil").tone("info").build(),
                        StatListItem.builder().label("Mid-Term — Science").value(dateLabel(9)).icon("pi-pencil").tone("info").build(),
                        StatListItem.builder().label("Unit Test — English").value(dateLabel(14)).icon("pi-pencil").tone("warning").build()
                ))
                .build();
    }

    public LibrarySummaryData librarySummary() {
        return LibrarySummaryData.builder()
                .booksIssued(3)
                .booksOverdue(1)
                .fineDue(15.0)
                .build();
    }

    public TransportSummaryData transportSummary() {
        return TransportSummaryData.builder()
                .routeName("Route 7 — Lakeview")
                .vehicleNumber("SCH-1042")
                .pickupTime("07:45 AM")
                .dropTime("02:30 PM")
                .liveStatus("On Time")
                .build();
    }

    public SupportTicketsData supportTickets() {
        return SupportTicketsData.builder()
                .openCount(3)
                .items(List.of(
                        TicketItem.builder().subject("Unable to reset staff password").status("Open").priority("High").raisedAgo("2h ago").build(),
                        TicketItem.builder().subject("Report export stuck at 90%").status("In Progress").priority("Medium").raisedAgo("1d ago").build(),
                        TicketItem.builder().subject("Add new subdomain alias").status("Open").priority("Low").raisedAgo("3d ago").build()
                ))
                .build();
    }

    /** Realistic-looking placeholder trend for fee/revenue charts, in the given chart type. */
    public ChartData trendChart(String seriesName, String chartType, double[] values) {
        List<String> labels = new ArrayList<>();
        List<Double> data = new ArrayList<>();
        LocalDate now = LocalDate.now();
        for (int i = values.length - 1; i >= 0; i--) {
            YearMonth ym = YearMonth.from(now.minusMonths(i));
            labels.add(ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
            data.add(values[values.length - 1 - i]);
        }
        return ChartData.builder()
                .chartType(chartType)
                .labels(labels)
                .series(List.of(ChartSeries.builder().name(seriesName).data(data).build()))
                .build();
    }

    private String dateLabel(int daysFromNow) {
        return LocalDate.now().plusDays(daysFromNow).toString();
    }
}
