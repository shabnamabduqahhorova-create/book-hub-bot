package com.bookhub.dto.dashboard;

import java.util.List;

public record DashboardStatisticsDto(
        long totalBooks,
        long totalUsers,
        long activeReaders,
        long completedReadings,
        List<BookMetricDto> mostReadBooks,
        List<BookMetricDto> highestRatedBooks
) {
}
