/**
 * AnomalyDetector.java
 *
 * Scans all data and flags any reading that looks wrong.
 *
 * 5 checks:
 *  1. MISSING ALTITUDE     — empty cell in CSV (stored as NaN)
 *  2. ALTITUDE SPIKE       — altitude jumps > 200m in one step
 *                            (skip if one of the rows is APOGEE or APOGEE_1 — those are real)
 *  3. ALTITUDE WRONG DIR   — drops during ASCENT or rises during DESCENT
 *                            (only checked within the same segment)
 *  4. BATTERY INCREASE     — voltage goes UP (impossible mid-flight)
 *  5. TEMPERATURE Z-SCORE  — temp is > 2.5 standard deviations from mean
 *
 * What is a z-score?
 *   z = (value - mean) / standardDeviation
 *   |z| > 2.5 means the value is an unusual outlier.
 */

import java.util.ArrayList;

public class AnomalyDetector {

    private static final double SPIKE_THRESHOLD     = 200.0;
    private static final double ZSCORE_THRESHOLD    = 2.5;
    private static final double DIRECTION_TOLERANCE = 5.0;

    public static void detect(ArrayList<FlightData> records) {

        System.out.println("\n========================================");
        System.out.println("         ANOMALY DETECTION REPORT");
        System.out.println("========================================");

        int anomalyCount = 0;

        // ── Check 1, 2, 3: Altitude checks ───────────────────────────────────
        for (int i = 0; i < records.size(); i++) {
            FlightData current = records.get(i);

            // Check 1: Missing altitude
            if (current.isMissingAltitude()) {
                flag(current, "MISSING altitude value (empty in CSV)");
                anomalyCount++;
                continue;
            }

            if (i == 0) continue;
            FlightData previous = records.get(i - 1);
            if (previous.isMissingAltitude()) continue;

            double altChange = current.altitude - previous.altitude;

            // Check 2: Spike — but only flag if NEITHER row is an apogee
            // (The big jump to/from the apogee is real data, not a spike)
            boolean currentIsApogee  = current.phase.equals("APOGEE")   || current.phase.equals("APOGEE_1");
            boolean previousIsApogee = previous.phase.equals("APOGEE")  || previous.phase.equals("APOGEE_1");

            if (!currentIsApogee && !previousIsApogee) {
                if (Math.abs(altChange) > SPIKE_THRESHOLD) {
                    flag(current, String.format(
                        "ALTITUDE SPIKE — jumped %.1fm in one step (%.1f → %.1f m)",
                        altChange, previous.altitude, current.altitude
                    ));
                    anomalyCount++;
                    continue;
                }
            }

            // Check 3: Wrong direction — only check within the same segment
            // Skip if either row is LANDED (phase transition is expected there)
            boolean crossingGap = current.phase.equals("LANDED") || previous.phase.equals("LANDED")
                                  || current.phase.equals("ASCENT") && previous.phase.equals("DESCENT");

            if (!crossingGap) {
                if (current.phase.equals("ASCENT") && altChange < -DIRECTION_TOLERANCE) {
                    flag(current, String.format(
                        "ALTITUDE DROPPED during ascent (%.1f → %.1f m)",
                        previous.altitude, current.altitude
                    ));
                    anomalyCount++;
                }
                if (current.phase.equals("DESCENT") && altChange > DIRECTION_TOLERANCE) {
                    flag(current, String.format(
                        "ALTITUDE ROSE during descent (%.1f → %.1f m)",
                        previous.altitude, current.altitude
                    ));
                    anomalyCount++;
                }
            }
        }

        // ── Check 4: Battery should never increase ────────────────────────────
        for (int i = 1; i < records.size(); i++) {
            FlightData current  = records.get(i);
            FlightData previous = records.get(i - 1);
            if (current.batteryVoltage > previous.batteryVoltage) {
                flag(current, String.format(
                    "BATTERY INCREASED (%.2fV → %.2fV) — impossible during flight",
                    previous.batteryVoltage, current.batteryVoltage
                ));
                anomalyCount++;
            }
        }

        // ── Check 5: Temperature z-score ──────────────────────────────────────
        double sum = 0;
        for (FlightData row : records) sum += row.temperature;
        double mean = sum / records.size();

        double sumSq = 0;
        for (FlightData row : records) {
            double diff = row.temperature - mean;
            sumSq += diff * diff;
        }
        double stdDev = Math.sqrt(sumSq / records.size());

        for (FlightData row : records) {
            double z = Math.abs((row.temperature - mean) / stdDev);
            if (z > ZSCORE_THRESHOLD) {
                flag(row, String.format(
                    "TEMPERATURE OUTLIER — %.1f°C has z-score=%.2f (threshold=%.1f)",
                    row.temperature, z, ZSCORE_THRESHOLD
                ));
                anomalyCount++;
            }
        }

        // ── Print results ─────────────────────────────────────────────────────
        for (FlightData row : records) {
            if (!row.anomalyNote.isEmpty()) {
                System.out.printf("  ⚠️  t=%-3d | %s%n", row.timestamp, row.anomalyNote);
            }
        }

        if (anomalyCount == 0) {
            System.out.println("  ✅ No anomalies detected!");
        } else {
            System.out.printf("%n  Total anomalies found: %d%n", anomalyCount);
        }

        System.out.printf("%n  Temperature stats: mean=%.2f°C, stdDev=%.2f°C%n", mean, stdDev);
    }

    private static void flag(FlightData row, String note) {
        if (row.anomalyNote.isEmpty()) {
            row.anomalyNote = note;
        } else {
            row.anomalyNote += " | " + note;
        }
    }
}
