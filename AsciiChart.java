/**
 * AsciiChart.java
 *
 * Renders ASCII terminal charts for altitude, temperature, and battery
 * data across the full flight timeline.
 */

import java.util.ArrayList;

public class AsciiChart {

    private static final int CHART_WIDTH  = 96;  // one column per data point
    private static final int CHART_HEIGHT = 20;  // rows tall

    /**
     * Prints all three charts: altitude, temperature, battery.
     */
    public static void printAllCharts(ArrayList<FlightData> records) {
        System.out.println("\n========================================");
        System.out.println("         ASCII TELEMETRY CHARTS");
        System.out.println("========================================");

        printAltitudeChart(records);
        printTemperatureChart(records);
        printBatteryChart(records);
    }

    // ── Altitude chart ─────────────────────────────────────────────────────

    public static void printAltitudeChart(ArrayList<FlightData> records) {
        System.out.println("\n  [ALTITUDE vs TIME]  (m)");
        System.out.println("  Anomaly markers: ! = spike/rise   ? = missing/fixed\n");

        double[] values = new double[records.size()];
        char[]   markers = new char[records.size()];
        for (int i = 0; i < records.size(); i++) {
            values[i]  = records.get(i).altitude;
            String note = records.get(i).anomalyNote;
            if (note.contains("SPIKE"))        markers[i] = '!';
            else if (note.contains("ROSE"))    markers[i] = '!';
            else if (note.contains("MISSING")) markers[i] = '?';
            else if (note.contains("FIXED"))   markers[i] = '*';
            else                               markers[i] = ' ';
        }

        renderChart(values, markers, "m", records);
        printPhaseBar(records);
    }

    // ── Temperature chart ──────────────────────────────────────────────────

    public static void printTemperatureChart(ArrayList<FlightData> records) {
        System.out.println("\n\n  [TEMPERATURE vs TIME]  (°C)");
        System.out.println("  Anomaly markers: ! = outlier\n");

        double[] values  = new double[records.size()];
        char[]   markers = new char[records.size()];
        for (int i = 0; i < records.size(); i++) {
            values[i]  = records.get(i).temperature;
            String note = records.get(i).anomalyNote;
            markers[i] = note.contains("TEMPERATURE") ? '!' : ' ';
        }

        renderChart(values, markers, "°C", records);
    }

    // ── Battery chart ──────────────────────────────────────────────────────

    public static void printBatteryChart(ArrayList<FlightData> records) {
        System.out.println("\n\n  [BATTERY VOLTAGE vs TIME]  (V)");
        System.out.println("  Steady drain expected during flight.\n");

        double[] values  = new double[records.size()];
        char[]   markers = new char[records.size()];
        for (int i = 0; i < records.size(); i++) {
            values[i]  = records.get(i).batteryVoltage;
            markers[i] = ' ';
        }

        renderChart(values, markers, "V", records);
    }

    // ── Core renderer ──────────────────────────────────────────────────────

    /**
     * Generic chart renderer.
     * @param values   one value per time step
     * @param markers  one marker char per time step (' ' = none)
     * @param unit     unit label for Y axis
     * @param records  full record list (for phase colouring on alt chart)
     */
    private static void renderChart(double[] values, char[] markers,
                                    String unit, ArrayList<FlightData> records) {

        int n = values.length;

        // Find min/max, ignoring NaN
        double minVal = Double.MAX_VALUE, maxVal = -Double.MAX_VALUE;
        for (double v : values) {
            if (!Double.isNaN(v)) {
                if (v < minVal) minVal = v;
                if (v > maxVal) maxVal = v;
            }
        }
        if (maxVal == minVal) maxVal = minVal + 1; // avoid div-by-zero

        // Build the grid: grid[row][col]
        char[][] grid = new char[CHART_HEIGHT][n];
        for (int r = 0; r < CHART_HEIGHT; r++)
            for (int c = 0; c < n; c++)
                grid[r][c] = ' ';

        // Place each data point
        for (int c = 0; c < n; c++) {
            if (Double.isNaN(values[c])) continue;
            int row = CHART_HEIGHT - 1
                    - (int) Math.round((values[c] - minVal) / (maxVal - minVal) * (CHART_HEIGHT - 1));
            row = Math.max(0, Math.min(CHART_HEIGHT - 1, row));
            grid[row][c] = (markers[c] != ' ') ? markers[c] : '█';
        }

        // Print Y axis labels + grid
        for (int r = 0; r < CHART_HEIGHT; r++) {
            // Y-axis label every ~5 rows
            double yVal = maxVal - (double) r / (CHART_HEIGHT - 1) * (maxVal - minVal);
            if (r % 5 == 0) {
                System.out.printf("  %8.1f %s |", yVal, unit);
            } else {
                System.out.print("           |");
            }

            for (int c = 0; c < n; c++) {
                System.out.print(grid[r][c]);
            }
            System.out.println();
        }

        // X axis line
        System.out.print("           +");
        for (int c = 0; c < n; c++) System.out.print("-");
        System.out.println();

        // X axis tick labels (every 10 steps)
        System.out.print("            ");
        for (int c = 0; c < n; c++) {
            if (c % 10 == 0) {
                String label = String.valueOf(c + 1);
                System.out.print(label);
                c += label.length() - 1;
            } else {
                System.out.print(" ");
            }
        }
        System.out.println("  → TIME (s)");
    }

    // ── Phase bar ──────────────────────────────────────────────────────────

    /**
     * Prints a one-line colour bar below the altitude chart showing flight phases.
     */
    private static void printPhaseBar(ArrayList<FlightData> records) {
        System.out.print("\n  PHASE: ");
        for (FlightData row : records) {
            switch (row.phase) {
                case "ASCENT":   System.out.print("^"); break;
                case "APOGEE":   System.out.print("A"); break;
                case "APOGEE_1": System.out.print("a"); break;
                case "DESCENT":  System.out.print("v"); break;
                case "LANDED":   System.out.print("_"); break;
                default:         System.out.print("."); break;
            }
        }
        System.out.println();
        System.out.println("         (^ ascent  A apogee  v descent  _ landed)");
    }
}
