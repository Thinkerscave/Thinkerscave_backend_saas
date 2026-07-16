package com.thinkerscave.dashboard.util;

import com.thinkerscave.dashboard.dto.response.widgetdata.ChartData;
import com.thinkerscave.dashboard.dto.response.widgetdata.ChartSeries;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Shared helper for turning a list of entity creation timestamps into a
 * month-bucketed {@link ChartData} payload (used by Organization/Student/
 * User growth and admission-trend charts across several dashboards).
 */
public final class ChartBucketUtil {

    private ChartBucketUtil() {}

    public static ChartData monthlyCounts(List<LocalDateTime> timestamps, int months, String seriesName, String chartType) {
        Map<String, Long> byMonth = timestamps.stream()
                .filter(t -> t != null)
                .collect(Collectors.groupingBy(t -> YearMonth.from(t).toString(), Collectors.counting()));

        List<String> labels = new ArrayList<>();
        List<Double> counts = new ArrayList<>();
        LocalDate now = LocalDate.now();
        for (int i = months - 1; i >= 0; i--) {
            YearMonth ym = YearMonth.from(now.minusMonths(i));
            labels.add(ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
            counts.add(byMonth.getOrDefault(ym.toString(), 0L).doubleValue());
        }

        return ChartData.builder()
                .chartType(chartType)
                .labels(labels)
                .series(List.of(ChartSeries.builder().name(seriesName).data(counts).build()))
                .build();
    }

    public static ChartData cumulativeCounts(List<LocalDateTime> timestamps, long baselineBeforeWindow, int months,
                                              String seriesName, String chartType) {
        ChartData monthly = monthlyCounts(timestamps, months, seriesName, chartType);
        List<Double> cumulative = new ArrayList<>();
        double running = baselineBeforeWindow;
        for (Double v : monthly.getSeries().get(0).getData()) {
            running += v;
            cumulative.add(running);
        }
        return ChartData.builder()
                .chartType(chartType)
                .labels(monthly.getLabels())
                .series(List.of(ChartSeries.builder().name(seriesName).data(cumulative).build()))
                .build();
    }
}
