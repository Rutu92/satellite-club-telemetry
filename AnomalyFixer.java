/**
 * AnomalyFixer.java
 *
 * This class FIXES the anomalies that AnomalyDetector found.
 *
 * In real aerospace engineering, you don't just delete bad data —
 * you replace it with an estimated value using the surrounding good data.
 * This is called INTERPOLATION.
 *
 * What is linear interpolation?
 *   If you have a reading at t=5 (value=50) and t=7 (value=70),
 *   but t=6 is missing or bad — you estimate it as the midpoint: 60.
 *   Formula: fixedValue = (prevValue + nextValue) / 2
 *
 * What we fix:
 *   1. MISSING altitude (NaN)  — replaced with linear interpolation
 *   2. ALTITUDE SPIKE          — interpolate from neighbors
 *                                EXCEPTION: we NEVER touch the APOGEE row
 *                                because its high altitude is REAL!
 *   3. ZERO during DESCENT     — altitude=0 while clearly airborne → interpolate
 *
 * We do NOT change temperature or battery — those anomalies are noted but
 * the values are physically plausible and don't need correction.
 */

import java.util.ArrayList;

public class AnomalyFixer {

    // If altitude changes by more than this in one step, we consider it a spike
    private static final double SPIKE_THRESHOLD = 200.0;

    /**
     * Fixes altitude anomalies in-place.
     * Rows that are fixed will have "[FIXED]" appended to their anomalyNote.
     *
     * @param records  the list of FlightData (anomalies already detected)
     */
    public static void fixAltitude(ArrayList<FlightData> records) {

        System.out.println("\n========================================");
        System.out.println("         ANOMALY TREATMENT REPORT");
        System.out.println("========================================");

        int fixedCount = 0;

        for (int i = 0; i < records.size(); i++) {
            FlightData current = records.get(i);

            // ── IMPORTANT: Never fix the APOGEE row ───────────────────────────
            // The apogee has a genuinely high altitude — it's not a spike.
            // Fixing it would destroy the most important data point in the dataset.
            if (current.phase.equals("APOGEE")) continue;

            boolean needsFix = false;

            // Condition 1: altitude is missing (NaN from empty CSV cell)
            if (current.isMissingAltitude()) {
                needsFix = true;
            }

            // Condition 2: altitude spiked vs previous row
            // We skip comparison if the previous row was the APOGEE (its jump is real)
            if (!needsFix && i > 0) {
                FlightData previous = records.get(i - 1);
                if (!previous.isMissingAltitude() && !previous.phase.equals("APOGEE")) {
                    double change = Math.abs(current.altitude - previous.altitude);
                    if (change > SPIKE_THRESHOLD) {
                        needsFix = true;
                    }
                }
            }

            // Condition 3: altitude is 0.0 during DESCENT while craft is clearly airborne
            // Confirmed bad if both neighbors show altitude > 10m
            if (!needsFix && current.altitude == 0.0 && current.phase.equals("DESCENT")) {
                double prevAlt = findPreviousValidAltitude(records, i);
                double nextAlt = findNextValidAltitude(records, i);
                if (!Double.isNaN(prevAlt) && prevAlt > 10.0 &&
                    !Double.isNaN(nextAlt) && nextAlt > 10.0) {
                    needsFix = true;
                }
            }

            if (needsFix) {
                double originalValue = current.altitude;

                double prevAlt = findPreviousValidAltitude(records, i);
                double nextAlt = findNextValidAltitude(records, i);

                double fixedAlt;

                if (!Double.isNaN(prevAlt) && !Double.isNaN(nextAlt)) {
                    fixedAlt = (prevAlt + nextAlt) / 2.0;   // interpolate midpoint
                } else if (!Double.isNaN(prevAlt)) {
                    fixedAlt = prevAlt;                       // carry forward
                } else if (!Double.isNaN(nextAlt)) {
                    fixedAlt = nextAlt;                       // use next
                } else {
                    System.out.printf("  ⚠️  t=%-3d | Cannot fix — no valid neighbors found%n",
                                      current.timestamp);
                    continue;
                }

                current.altitude = fixedAlt;

                String fixNote = String.format(
                    "[FIXED: altitude was %s → interpolated to %.1f m]",
                    Double.isNaN(originalValue) ? "MISSING" : String.format("%.1f", originalValue),
                    fixedAlt
                );
                current.anomalyNote += " " + fixNote;

                System.out.printf("  ✅ t=%-3d | %s%n", current.timestamp, fixNote);
                fixedCount++;
            }
        }

        if (fixedCount == 0) {
            System.out.println("  No altitude fixes needed.");
        } else {
            System.out.printf("%n  Total rows fixed: %d%n", fixedCount);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Searches BACKWARDS from position i to find the nearest clean altitude.
     * Skips anomaly rows — but APOGEE is always treated as valid.
     */
    private static double findPreviousValidAltitude(ArrayList<FlightData> records, int fromIndex) {
        for (int i = fromIndex - 1; i >= 0; i--) {
            FlightData row = records.get(i);
            if (!row.isMissingAltitude() &&
                (row.anomalyNote.isEmpty() || row.phase.equals("APOGEE"))) {
                return row.altitude;
            }
        }
        return Double.NaN;
    }

    /**
     * Searches FORWARD from position i to find the nearest clean altitude.
     * Skips anomaly rows — but APOGEE is always treated as valid.
     */
    private static double findNextValidAltitude(ArrayList<FlightData> records, int fromIndex) {
        for (int i = fromIndex + 1; i < records.size(); i++) {
            FlightData row = records.get(i);
            if (!row.isMissingAltitude() &&
                (row.anomalyNote.isEmpty() || row.phase.equals("APOGEE"))) {
                return row.altitude;
            }
        }
        return Double.NaN;
    }
}
