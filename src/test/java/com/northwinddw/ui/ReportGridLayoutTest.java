package com.northwinddw.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportGridLayoutTest {
    @Test
    void usesThreeColumnsAtDesktopWidth() {
        ReportGridLayout layout = ReportGridLayout.calculate(1480);

        assertEquals(3, layout.columns());
        assertEquals(461.333, layout.cardWidth(), 0.01);
        assertTrue(layout.chartHeight() > 0);
    }

    @Test
    void usesTwoColumnsAtMediumWidth() {
        ReportGridLayout layout = ReportGridLayout.calculate(1000);

        assertEquals(2, layout.columns());
        assertEquals(460, layout.cardWidth(), 0.01);
        assertTrue(layout.chartHeight() > 0);
    }

    @Test
    void usesOneColumnAndKeepsChartsReadableAtNarrowWidth() {
        ReportGridLayout layout = ReportGridLayout.calculate(800);

        assertEquals(1, layout.columns());
        assertEquals(736, layout.cardWidth(), 0.01);
        assertTrue(layout.chartHeight() >= 238);
    }

    @Test
    void keepsChartHeightPositiveForSmallestSupportedViewport() {
        ReportGridLayout layout = ReportGridLayout.calculate(320);

        assertEquals(1, layout.columns());
        assertTrue(layout.cardWidth() >= 300);
        assertTrue(layout.chartHeight() > 0);
    }
}
