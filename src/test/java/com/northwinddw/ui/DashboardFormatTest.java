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
    void labelsAnUnfilteredDashboardAndASelectedYear() {
        assertEquals("Todos los años", DashboardFormat.yearLabel(null));
        assertEquals("1997", DashboardFormat.yearLabel(1997));
    }

    @Test
    void formatsCategoryShareAsOneDecimalPlace() {
        assertEquals("8.1%", DashboardFormat.percentage(new BigDecimal("8.12")));
    }
}
