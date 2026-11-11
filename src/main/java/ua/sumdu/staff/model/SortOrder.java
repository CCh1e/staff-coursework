package ua.sumdu.staff.model;

/**
 * Напрям сортування.
 */
public enum SortOrder {
    ASC("За зростанням", "ASC"),
    DESC("За спаданням", "DESC");

    private final String title;
    private final String sql;

    SortOrder(String title, String sql) {
        this.title = title;
        this.sql = sql;
    }

    /** Ключове слово SQL; береться лише з переліку, тому безпечне для підстановки в запит. */
    public String sql() {
        return sql;
    }

    @Override
    public String toString() {
        return title;
    }
}
