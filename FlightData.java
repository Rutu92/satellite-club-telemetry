/**
 * FlightData.java
 *
 * This is a "model" class — it represents ONE row from the CSV file.
 * Think of it like a blueprint. Every row of data becomes one FlightData object.
 *
 * Each object stores:
 *   - timestamp       : the time in seconds (1, 2, 3 ... 96)
 *   - altitude        : height in metres (can be MISSING — stored as NaN)
 *   - temperature     : in Celsius
 *   - batteryVoltage  : in Volts
 *   - phase           : "ASCENT", "APOGEE", or "DESCENT" (set later by FlightAnalyzer)
 *   - anomalyNote     : description of any anomaly found at this row
 */
public class FlightData {

    // ── Fields ────────────────────────────────────────────────────────────────
    public int    timestamp;
    public double altitude;        // Double.NaN means the value was missing in the CSV
    public double temperature;
    public double batteryVoltage;
    public String phase;           // Set by FlightAnalyzer
    public String anomalyNote;     // Set by AnomalyDetector ("" if none)

    // ── Constructor ───────────────────────────────────────────────────────────
    // A constructor is called when you write: new FlightData(...)
    // It sets up the object with all its starting values.
    public FlightData(int timestamp, double altitude, double temperature, double batteryVoltage) {
        this.timestamp      = timestamp;
        this.altitude       = altitude;
        this.temperature    = temperature;
        this.batteryVoltage = batteryVoltage;
        this.phase          = "UNKNOWN";
        this.anomalyNote    = "";
    }

    // ── Helper methods ────────────────────────────────────────────────────────

    // Returns true if altitude is missing (NaN = Not a Number)
    public boolean isMissingAltitude() {
        return Double.isNaN(this.altitude);
    }

    // toString() is called automatically when you print an object
    // e.g. System.out.println(someFlightData) will use this
    @Override
    public String toString() {
        String altStr = isMissingAltitude() ? "MISSING" : String.format("%.1f m", altitude);
        return String.format(
            "t=%2d | alt=%-10s | temp=%.1f°C | batt=%.2fV | phase=%-8s | %s",
            timestamp, altStr, temperature, batteryVoltage, phase,
            anomalyNote.isEmpty() ? "OK" : "⚠ " + anomalyNote
        );
    }
}
