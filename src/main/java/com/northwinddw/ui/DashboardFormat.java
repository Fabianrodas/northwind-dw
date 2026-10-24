package com.northwinddw.ui;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public final class DashboardFormat {
    private DashboardFormat() {
    }

    public static String usd(BigDecimal amount) {
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.US);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format.format(amount);
    }

    public static String percentage(BigDecimal amount) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
        format.setMinimumFractionDigits(1);
        format.setMaximumFractionDigits(1);
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format.format(amount) + "%";
    }

    public static String yearLabel(Integer year) {
        return year == null ? "Todos los años" : year.toString();
    }
}
