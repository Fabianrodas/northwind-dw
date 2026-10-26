package com.northwinddw.ui;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
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

    public static String chartUsd(BigDecimal amount) {
        BigDecimal absolute = amount.abs();
        String sign = amount.signum() < 0 ? "-$" : "$";
        if (absolute.compareTo(new BigDecimal("1000000")) >= 0) {
            return sign + compact(absolute.movePointLeft(6), "0.##") + "M";
        }
        if (absolute.compareTo(new BigDecimal("1000")) >= 0) {
            return sign + compact(absolute.movePointLeft(3), "0.#") + "K";
        }

        NumberFormat format = NumberFormat.getIntegerInstance(Locale.US);
        format.setRoundingMode(RoundingMode.HALF_UP);
        return sign + format.format(absolute);
    }

    public static String quarterLabel(String sourceQuarter) {
        String value = sourceQuarter.trim();
        String normalized = value.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "first", "1", "t1", "q1" -> "T1";
            case "second", "2", "t2", "q2" -> "T2";
            case "third", "3", "t3", "q3" -> "T3";
            case "fourth", "4", "t4", "q4" -> "T4";
            default -> value;
        };
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

    private static String compact(BigDecimal amount, String pattern) {
        DecimalFormat format = new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.US));
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format.format(amount);
    }
}
