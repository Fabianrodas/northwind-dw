package com.northwinddw.model;

import java.math.BigDecimal;

public record RevenueSummary(BigDecimal totalRevenue, long customers, long suppliers) {
}
