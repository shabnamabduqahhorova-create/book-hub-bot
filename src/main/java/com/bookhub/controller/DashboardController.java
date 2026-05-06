package com.bookhub.controller;

import com.bookhub.dto.dashboard.BookMetricDto;
import com.bookhub.dto.dashboard.DashboardStatisticsDto;
import com.bookhub.service.DashboardService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/statistics")
    public DashboardStatisticsDto statistics() {
        return dashboardService.statistics();
    }

    @GetMapping("/most-read-books")
    public List<BookMetricDto> mostReadBooks() {
        return dashboardService.mostReadBooks();
    }

    @GetMapping("/highest-rated-books")
    public List<BookMetricDto> highestRatedBooks() {
        return dashboardService.highestRatedBooks();
    }
}
