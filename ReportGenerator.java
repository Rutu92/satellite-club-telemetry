/**
 * ReportGenerator.java
 *
 * This class produces the final human-readable report.
 *
 * It prints:
 *   1. Per-phase statistics (min, max, mean for each sensor)
 *   2. A complete table of all rows showing phase + any anomaly/fix notes
 *   3. A final summary of what was found and fixed
 */

import java.util.ArrayList;

public class ReportGenerator {

    /**
     * Prints the complete analysis report to the console.
     *
     * @param records  the fully labelled, detected, and fixed list of FlightData
     */
    public static void printReport(ArrayList<FlightData> records) {

        System.out.println("\n========================================");
        System.out.println("      FULL TELEMETRY ANALYSIS REPORT");
        System.out.println("========================================");

        // ── Section 1: Statistics per phase ──────────────────────────────────
        System.out.println("\n--- STATISTICS BY FLIGHT PHASE ---\n");

        String[] phases = {"ASCENT", "APOGEE", "DESCENT"};

        for (String phase : phases) {
            // Collect all rows for this phase
            ArrayList<FlightData> phaseRows = new ArrayList<>();
            for (FlightData row : records) {
                if (row.phase.equals(phase)) phaseRows.add(row);
            }

            if (phaseRows.isEmpty()) continue;

            System.out.println("  Phase: " + phase + " (" + phaseRows.size() + " data points)");

            // Altitude stats (skip NaN values)
            double altMin = Double.MAX_VALUE, altMax = Double.MIN_VALUE, altSum = 0;
            int    altCount = 0;
            for (FlightData row : phaseRows) {
                if (!row.isMissingAltitude()) {
                    altMin    = Math.min(altMin, row.altitude);
                    altMax    = Math.max(altMax, row.altitude);
                    altSum   += row.altitude;
                    altCount++;
                }
            }
            System.out.printf("    Altitude     : min=%.1fm  max=%.1fm  mean=%.1fm%n",
                              altMin, altMax, altSum / altCount);

            // Temperature stats
            double tMin = Double.MAX_VALUE, tMax = Double.MIN_VALUE, tSum = 0;
            for (FlightData row : phaseRows) {
                tMin  = Math.min(tMin, row.temperature);
                tMax  = Math.max(tMax, row.temperature);
                tSum += row.temperature;
            }
            System.out.printf("    Temperature  : min=%.1f°C  max=%.1f°C  mean=%.1f°C%n",
                              tMin, tMax, tSum / phaseRows.size());

            // Battery stats
            double bMin = Double.MAX_VALUE, bMax = Double.MIN_VALUE, bSum = 0;
            for (FlightData row : phaseRows) {
                bMin  = Math.min(bMin, row.batteryVoltage);
                bMax  = Math.max(bMax, row.batteryVoltage);
                bSum += row.batteryVoltage;
            }
            System.out.printf("    Battery      : min=%.2fV   max=%.2fV   mean=%.2fV%n%n",
                              bMin, bMax, bSum / phaseRows.size());
        }

        // ── ASCII Charts ──────────────────────────────────────────────────────
        AsciiChart.printAllCharts(records);

        // ── Section 2: Full data table ────────────────────────────────────────
        System.out.println("\n--- COMPLETE DATA TABLE ---\n");
        System.out.printf("  %-5s  %-10s  %-12s  %-12s  %-9s  %s%n",
                          "TIME", "ALT(m)", "TEMP(°C)", "BATT(V)", "PHASE", "STATUS/NOTES");
        System.out.println("  " + "-".repeat(90));

        for (FlightData row : records) {
            String altStr = row.isMissingAltitude() ? "MISSING" : String.format("%.1f", row.altitude);
            String status = row.anomalyNote.isEmpty() ? "OK" : "⚠ " + row.anomalyNote;

            // Truncate long notes so the table stays readable
            if (status.length() > 60) status = status.substring(0, 57) + "...";

            System.out.printf("  %-5d  %-10s  %-12.1f  %-12.2f  %-9s  %s%n",
                              row.timestamp, altStr,
                              row.temperature, row.batteryVoltage,
                              row.phase, status);
        }

        // ── Section 3: Final summary counts ──────────────────────────────────
        System.out.println("\n--- FINAL SUMMARY ---\n");

        int totalAnomalies = 0, totalFixed = 0;
        for (FlightData row : records) {
            if (!row.anomalyNote.isEmpty()) totalAnomalies++;
            if (row.anomalyNote.contains("[FIXED")) totalFixed++;
        }

        System.out.printf("  Total rows        : %d%n", records.size());
        System.out.printf("  Rows with anomaly : %d%n", totalAnomalies);
        System.out.printf("  Rows auto-fixed   : %d%n", totalFixed);
        System.out.printf("  Rows still flagged: %d%n", totalAnomalies - totalFixed);
        System.out.println("\n  ✅ Analysis complete.");
    }
}
