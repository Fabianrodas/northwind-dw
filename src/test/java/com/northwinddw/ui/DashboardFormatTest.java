package com.northwinddw.ui;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DashboardFormatTest {
    @Test
    void formatsRevenueAsUsdWithoutDroppingCents() {
        assertEquals("$1,265,792.94", DashboardFormat.usd(new BigDecimal("1265792.94")));
    }

    @Test
    void formatsChartValuesCompactlyWithoutChangingExactTooltipValues() {
        assertEquals("$0", DashboardFormat.chartUsd(new BigDecimal("0.00")));
        assertEquals("$98", DashboardFormat.chartUsd(new BigDecimal("98.37")));
        assertEquals("$1.2K", DashboardFormat.chartUsd(new BigDecimal("1234.56")));
        assertEquals("-$1.2K", DashboardFormat.chartUsd(new BigDecimal("-1234.56")));
        assertEquals("$267.9K", DashboardFormat.chartUsd(new BigDecimal("267868.16")));
        assertEquals("$1.27M", DashboardFormat.chartUsd(new BigDecimal("1265792.94")));
        assertEquals("$1,265,792.94", DashboardFormat.usd(new BigDecimal("1265792.94")));
    }

    @Test
    void labelsAnUnfilteredDashboardAndASelectedYear() {
        assertEquals("Todos los años", DashboardFormat.yearLabel(null));
        assertEquals("1997", DashboardFormat.yearLabel(1997));
    }

    @Test
    void formatsCategoryShareAsOneDecimalPlace() {
        assertEquals("8.1%", DashboardFormat.percentage(new BigDecimal("8.12")));
    }

    @Test
    void normalizesTheFourEnglishAndSpanishQuarterLabelsInOrdinalOrder() {
        assertEquals("T1", DashboardFormat.quarterLabel("First"));
        assertEquals("T2", DashboardFormat.quarterLabel("Second"));
        assertEquals("T3", DashboardFormat.quarterLabel("Third"));
        assertEquals("T4", DashboardFormat.quarterLabel("Fourth"));
        assertEquals("T1", DashboardFormat.quarterLabel("Q1"));
        assertEquals("T4", DashboardFormat.quarterLabel("t4"));
        assertEquals("T2", DashboardFormat.quarterLabel(" 2 "));
        assertEquals("T3", DashboardFormat.quarterLabel("T3"));
        assertEquals("Quarter 5", DashboardFormat.quarterLabel(" Quarter 5 "));
    }
}
