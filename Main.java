/**
 * Main.java
 *
 * This is the ENTRY POINT of the program — the first thing Java runs.
 * Every Java program needs a class with a main() method.
 *
 * This class ties all the other classes together in the right order:
 *
 *   Step 1: Load the CSV data         → TelemetryLoader
 *   Step 2: Detect flight phases       → FlightAnalyzer
 *   Step 3: Detect anomalies           → AnomalyDetector
 *   Step 4: Fix / treat anomalies      → AnomalyFixer
 *   Step 5: Print the full report      → ReportGenerator
 *
 * HOW TO RUN:
 *   1. Put telemetry_dataset.csv in the same folder as this file
 *   2. Compile:  javac *.java
 *   3. Run:      java Main
 */

import java.util.ArrayList;

public class Main {

    public static void main(String[] args) {

        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║  SATELLITE TELEMETRY ANALYSIS SYSTEM ║");
        System.out.println("╚══════════════════════════════════════╝\n");

        // ── Step 1: Load data from CSV ────────────────────────────────────────
        // Change this path if your CSV is somewhere else
        String csvFile = "telemetry_dataset.csv";
        ArrayList<FlightData> records = TelemetryLoader.load(csvFile);

        if (records.isEmpty()) {
            System.out.println("❌ No data loaded. Check the file path and try again.");
            return;
        }

        // ── Step 2: Detect flight phases ──────────────────────────────────────
        // Labels every row as ASCENT, APOGEE, or DESCENT
        FlightAnalyzer.labelPhases(records);

        // ── Step 3: Detect anomalies ──────────────────────────────────────────
        // Checks for missing values, spikes, wrong direction, battery issues, z-scores
        AnomalyDetector.detect(records);

        // ── Step 4: Fix anomalies ─────────────────────────────────────────────
        // Replaces bad altitude values using linear interpolation
        AnomalyFixer.fixAltitude(records);

        // ── Step 5: Print full report ─────────────────────────────────────────
        // Shows stats per phase + complete data table + summary
        ReportGenerator.printReport(records);
    }
}
