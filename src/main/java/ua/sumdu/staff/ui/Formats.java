package ua.sumdu.staff.ui;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Форматування чисел і дат для відображення в таблицях.
 */
public final class Formats {

    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private static final DecimalFormatSymbols SYMBOLS = DecimalFormatSymbols.getInstance(Locale.of("uk", "UA"));

    private Formats() {
    }

    public static String money(BigDecimal value) {
        if (value == null) {
            return "";
        }
        DecimalFormat format = new DecimalFormat("#,##0.00", SYMBOLS);
        return format.format(value);
    }

    public static String percent(BigDecimal value) {
        return value == null ? "" : new DecimalFormat("0.00", SYMBOLS).format(value) + " %";
    }

    public static String date(LocalDate date) {
        return date == null ? "" : DATE.format(date);
    }
}
