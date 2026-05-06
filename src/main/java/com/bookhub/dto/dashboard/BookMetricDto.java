package com.bookhub.dto.dashboard;

import java.math.BigDecimal;

public record BookMetricDto(Long bookId, String title, long count, BigDecimal score) {
}
