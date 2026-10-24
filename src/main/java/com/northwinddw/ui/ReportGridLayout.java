package com.northwinddw.ui;

public record ReportGridLayout(int columns, double cardWidth, double cardHeight, double chartHeight) {
    private static final double HORIZONTAL_PADDING = 60;
    private static final double LAYOUT_SAFETY = 4;
    private static final double CARD_GAP = 16;
    private static final double THREE_COLUMN_BREAKPOINT = 1120;
    private static final double TWO_COLUMN_BREAKPOINT = 760;
    private static final double MINIMUM_CONTENT_WIDTH = 320;

    public static ReportGridLayout calculate(double paneWidth) {
        double safePaneWidth = Double.isFinite(paneWidth) ? Math.max(0, paneWidth) : 0;
        double contentWidth = Math.max(MINIMUM_CONTENT_WIDTH, safePaneWidth - HORIZONTAL_PADDING - LAYOUT_SAFETY);
        int columns = contentWidth >= THREE_COLUMN_BREAKPOINT ? 3
                : contentWidth >= TWO_COLUMN_BREAKPOINT ? 2 : 1;
        double cardWidth = (contentWidth - CARD_GAP * (columns - 1)) / columns;
        double cardHeight = Math.max(320, Math.min(400, cardWidth * 0.8));
        double chartHeight = cardHeight - 82;

        return new ReportGridLayout(columns, cardWidth, cardHeight, chartHeight);
    }
}
