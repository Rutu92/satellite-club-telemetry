/**
 * FlightAnalyzer.java
 *
 * This class detects flight phases for every data point.
 *
 * PHASES:
 *   ASCENT  — craft is going UP
 *   APOGEE  — the single highest point
 *   DESCENT — craft is coming DOWN
 *   LANDED  — craft is on the ground between two flight segments
 *
 * KEY INSIGHT — this dataset has TWO segments:
 *   Segment 1: t=1..23   craft goes up to ~915m then comes back down to 0
 *   Landed:    t=24..54  craft is on the ground (altitude near 0), preparing relaunch
 *   Segment 2: t=53..96  craft goes up to 1000m (true apogee) then descends
 *
 * How we detect this:
 *   1. Find the global apogee (highest altitude in the whole dataset)
 *   2. Everything after the apogee = DESCENT
 *   3. Working backwards from the apogee, find where ascent truly begins
 *      by detecting a "landed" gap — a run of rows near ground level (< 20m)
 *   4. Label that gap as LANDED
 *   5. Everything before the gap = first flight segment (ASCENT then DESCENT)
 */

import java.util.ArrayList;

public class FlightAnalyzer {

    // Altitude below this = "on the ground" / landed
    private static final double GROUND_THRESHOLD = 20.0;

    public static void labelPhases(ArrayList<FlightData> records) {

        // ── Step 1: Find the global apogee ────────────────────────────────────
        int    apogeeIndex = -1;
        double maxAlt      = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < records.size(); i++) {
            FlightData row = records.get(i);
            if (!row.isMissingAltitude() && row.altitude > maxAlt) {
                maxAlt      = row.altitude;
                apogeeIndex = i;
            }
        }

        if (apogeeIndex == -1) {
            System.out.println("❌ Could not find apogee!");
            return;
        }

        // ── Step 2: Label APOGEE and DESCENT ─────────────────────────────────
        records.get(apogeeIndex).phase = "APOGEE";
        for (int i = apogeeIndex + 1; i < records.size(); i++) {
            records.get(i).phase = "DESCENT";
        }

        // ── Step 3: Working backwards from apogee, find the "LANDED" gap ─────
        // A landed gap = consecutive rows where altitude < GROUND_THRESHOLD
        // We search backwards and find where ground-level rows start and end
        int landedEnd   = apogeeIndex - 1;
        int landedStart = landedEnd;

        // Walk backwards to find the start of the landed segment
        while (landedStart > 0) {
            FlightData row = records.get(landedStart - 1);
            double alt = row.isMissingAltitude() ? 0 : row.altitude;
            if (alt < GROUND_THRESHOLD) {
                landedStart--;
            } else {
                break;
            }
        }

        // ── Step 4: Label LANDED rows ─────────────────────────────────────────
        boolean foundLandedGap = (landedStart < landedEnd);

        if (foundLandedGap) {
            for (int i = landedStart; i <= landedEnd; i++) {
                records.get(i).phase = "LANDED";
            }
        }

        // ── Step 5: Label everything before the landed gap ───────────────────
        // Find the mini-apogee (peak) of the first segment
        int    firstApogeeIdx = -1;
        double firstMaxAlt    = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < landedStart; i++) {
            FlightData row = records.get(i);
            if (!row.isMissingAltitude() && row.altitude > firstMaxAlt) {
                firstMaxAlt    = row.altitude;
                firstApogeeIdx = i;
            }
        }

        if (firstApogeeIdx != -1) {
            for (int i = 0; i < landedStart; i++) {
                if      (i < firstApogeeIdx)  records.get(i).phase = "ASCENT";
                else if (i == firstApogeeIdx) records.get(i).phase = "APOGEE_1";
                else                           records.get(i).phase = "DESCENT";
            }
        }

        // Label the second ascent (between landed gap and main apogee)
        for (int i = landedEnd + 1; i < apogeeIndex; i++) {
            records.get(i).phase = "ASCENT";
        }

        // ── Step 6: Print summary ─────────────────────────────────────────────
        FlightData apogeeRow = records.get(apogeeIndex);

        System.out.println("\n========================================");
        System.out.println("         FLIGHT PHASE SUMMARY");
        System.out.println("========================================");
        System.out.printf("  Total data points   : %d%n", records.size());

        if (foundLandedGap && firstApogeeIdx != -1) {
            System.out.printf("  ── Segment 1 ──────────────────────────%n");
            System.out.printf("  Ascent              : rows 0 → %d%n", firstApogeeIdx - 1);
            System.out.printf("  First peak (APOGEE_1): row %d, t=%d, alt=%.1fm%n",
                              firstApogeeIdx,
                              records.get(firstApogeeIdx).timestamp,
                              records.get(firstApogeeIdx).altitude);
            System.out.printf("  Descent             : rows %d → %d%n",
                              firstApogeeIdx + 1, landedStart - 1);
            System.out.printf("  Landed              : rows %d → %d  (%d points on ground)%n",
                              landedStart, landedEnd, landedEnd - landedStart + 1);
            System.out.printf("  ── Segment 2 ──────────────────────────%n");
            System.out.printf("  Ascent              : rows %d → %d%n",
                              landedEnd + 1, apogeeIndex - 1);
        }

        System.out.printf("  APOGEE (main peak)  : row %d, t=%d, alt=%.1fm%n",
                          apogeeIndex, apogeeRow.timestamp, apogeeRow.altitude);
        System.out.printf("  Descent             : rows %d → %d  (%d points)%n",
                          apogeeIndex + 1, records.size() - 1,
                          records.size() - 1 - apogeeIndex);
    }
}
