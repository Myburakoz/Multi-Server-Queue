package util;

/**
 * ASCII box-drawing table printer with ANSI color support.
 * Uses ─ │ ┌ ┐ └ ┘ ┬ ┴ ├ ┤ ┼ characters for clean MySQL-style tables.
 */
public class TablePrinter {

    // ANSI color codes
    public static final String RESET   = "\033[0m";
    public static final String BOLD    = "\033[1m";
    public static final String DIM     = "\033[2m";

    // Foreground colors
    public static final String RED     = "\033[31m";
    public static final String GREEN   = "\033[32m";
    public static final String YELLOW  = "\033[33m";
    public static final String BLUE    = "\033[34m";
    public static final String MAGENTA = "\033[35m";
    public static final String CYAN    = "\033[36m";
    public static final String WHITE   = "\033[37m";

    // Bright foreground
    public static final String BRIGHT_RED     = "\033[91m";
    public static final String BRIGHT_GREEN   = "\033[92m";
    public static final String BRIGHT_YELLOW  = "\033[93m";
    public static final String BRIGHT_BLUE    = "\033[94m";
    public static final String BRIGHT_MAGENTA = "\033[95m";
    public static final String BRIGHT_CYAN    = "\033[96m";
    public static final String BRIGHT_WHITE   = "\033[97m";

    // Box drawing characters
    private static final char H  = '─'; // horizontal
    private static final char V  = '│'; // vertical
    private static final char TL = '┌'; // top-left
    private static final char TR = '┐'; // top-right
    private static final char BL = '└'; // bottom-left
    private static final char BR = '┘'; // bottom-right
    private static final char TJ = '┬'; // top junction
    private static final char BJ = '┴'; // bottom junction
    private static final char LJ = '├'; // left junction
    private static final char RJ = '┤'; // right junction
    private static final char CJ = '┼'; // cross junction

    // ─── Building methods ───

    /**
     * Builds a horizontal border line.
     * @param widths   column widths (content width, padding is added automatically)
     * @param left     left corner/junction char
     * @param mid      middle junction char
     * @param right    right corner/junction char
     * @param color    ANSI color for the border
     */
    public static String border(int[] widths, char left, char mid, char right, String color) {
        StringBuilder sb = new StringBuilder();
        sb.append(color);
        sb.append(left);
        for (int i = 0; i < widths.length; i++) {
            // +2 for padding on each side
            for (int j = 0; j < widths[i] + 2; j++) sb.append(H);
            if (i < widths.length - 1) sb.append(mid);
        }
        sb.append(right);
        sb.append(RESET);
        return sb.toString();
    }

    public static String topBorder(int[] widths, String color) {
        return border(widths, TL, TJ, TR, color);
    }

    public static String midBorder(int[] widths, String color) {
        return border(widths, LJ, CJ, RJ, color);
    }

    public static String bottomBorder(int[] widths, String color) {
        return border(widths, BL, BJ, BR, color);
    }

    /**
     * Builds a data row with colored borders and content.
     * @param widths       column widths
     * @param values       cell values (plain text, will be left-aligned)
     * @param cellColors   per-cell color (use "" or RESET for default)
     * @param borderColor  color for the │ separators
     */
    public static String row(int[] widths, String[] values, String[] cellColors, String borderColor) {
        StringBuilder sb = new StringBuilder();
        sb.append(borderColor).append(V).append(RESET);
        for (int i = 0; i < widths.length; i++) {
            String val = (i < values.length) ? values[i] : "";
            String col = (i < cellColors.length && cellColors[i] != null && !cellColors[i].isEmpty()) ? cellColors[i] : RESET;
            sb.append(' ');
            sb.append(col);
            sb.append(padRight(val, widths[i]));
            sb.append(RESET);
            sb.append(' ');
            sb.append(borderColor).append(V).append(RESET);
        }
        return sb.toString();
    }

    /**
     * Builds a row with uniform color for all cells.
     */
    public static String row(int[] widths, String[] values, String cellColor, String borderColor) {
        String[] colors = new String[values.length];
        for (int i = 0; i < colors.length; i++) colors[i] = cellColor;
        return row(widths, values, colors, borderColor);
    }

    /**
     * Prints a centered title banner.
     */
    public static void printTitle(String title, int totalWidth, String color) {
        int pad = (totalWidth - title.length()) / 2;
        if (pad < 0) pad = 0;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pad; i++) sb.append(' ');
        sb.append(color).append(BOLD).append(title).append(RESET);
        System.out.println(sb.toString());
    }

    /**
     * Computes total display width of the table given column widths.
     * Each column contributes: 1 (│) + 1 (pad) + width + 1 (pad) ... + final │
     */
    public static int totalWidth(int[] widths) {
        int w = 1; // leading │
        for (int cw : widths) {
            w += cw + 2 + 1; // pad-left + content + pad-right + │
        }
        return w;
    }

    // ─── Helpers ───

    private static String padRight(String s, int width) {
        if (s == null) s = "";
        if (s.length() >= width) return s.substring(0, width);
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < width) sb.append(' ');
        return sb.toString();
    }
}
