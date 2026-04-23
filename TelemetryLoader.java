/**
 * TelemetryLoader.java
 *
 * This class reads the CSV file and converts each row into a FlightData object.
 *
 * What it handles:
 *   - Skipping the header row (timestamp,altitude,temperature,battery_voltage)
 *   - Handling MISSING values (empty cells in the CSV become Double.NaN)
 *   - Storing all rows in an ArrayList (a resizable list)
 */

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class TelemetryLoader {

    /**
     * Reads a CSV file and returns a list of FlightData objects.
     *
     * @param filename  path to the CSV file (e.g. "telemetry_dataset.csv")
     * @return          ArrayList of FlightData, one per row
     */
    public static ArrayList<FlightData> load(String filename) {

        // ArrayList is like an array but it grows automatically as you add items
        ArrayList<FlightData> records = new ArrayList<>();

        try {
            // BufferedReader reads the file line by line efficiently
            BufferedReader reader = new BufferedReader(new FileReader(filename));

            String line;
            boolean firstLine = true;   // used to skip the header row

            while ((line = reader.readLine()) != null) {

                // Skip the header row (timestamp,altitude,temperature,battery_voltage)
                if (firstLine) {
                    firstLine = false;
                    continue;   // "continue" jumps to the next loop iteration
                }

                // Split the line by commas — gives us an array like:
                // ["1", "0.0", "25.0", "7.4"]
                String[] parts = line.split(",");

                // Parse each value from String to a number
                int    timestamp      = Integer.parseInt(parts[0].trim());
                double temperature    = Double.parseDouble(parts[2].trim());
                double batteryVoltage = Double.parseDouble(parts[3].trim());

                // Altitude might be MISSING (empty string in CSV)
                // If it's empty, we store Double.NaN to represent "no value"
                double altitude;
                String altRaw = parts[1].trim();
                if (altRaw.isEmpty()) {
                    altitude = Double.NaN;   // NaN = Not a Number = missing value
                } else {
                    altitude = Double.parseDouble(altRaw);
                }

                // Create one FlightData object for this row and add it to the list
                records.add(new FlightData(timestamp, altitude, temperature, batteryVoltage));
            }

            reader.close();
            System.out.println("✅ Loaded " + records.size() + " rows from " + filename);

        } catch (IOException e) {
            // This runs if the file isn't found or can't be read
            System.out.println("❌ Error reading file: " + e.getMessage());
        }

        return records;
    }
}
