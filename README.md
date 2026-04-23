# 🛰️ Satellite Club — Telemetry Analysis System

A Java command-line tool that loads rocket telemetry data from a CSV file, automatically detects flight phases and sensor anomalies, repairs bad altitude readings using linear interpolation, and prints a full structured analysis report.

---

## 📁 Project Structure

```
Satelite_club_project/
├── Main.java               # Entry point — runs all 5 pipeline steps in order
├── FlightData.java         # Data model — one object per CSV row
├── TelemetryLoader.java    # Reads and parses the CSV file
├── FlightAnalyzer.java     # Labels each row: ASCENT / APOGEE / DESCENT / LANDED
├── AnomalyDetector.java    # Detects 5 types of sensor anomaly
├── AnomalyFixer.java       # Fixes bad altitude values using interpolation
├── ReportGenerator.java    # Prints the full analysis report to the console
└── telemetry_dataset.csv   # Input data (96 rows, 1-second intervals)
```

---

## ⚙️ Requirements

- **Java JDK 17 or later** — [Download from Adoptium](https://adoptium.net)
- **VS Code** *(recommended)* — [Download here](https://code.visualstudio.com), with the [Extension Pack for Java](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack)

---

## 🚀 How to Run

**1. Open the project folder in VS Code**
```
File → Open Folder → select Satelite_club_project/
```

**2. Open the integrated terminal**
```
Terminal → New Terminal
```

**3. Compile all Java files**
```bash
javac *.java
```

**4. Run the program**
```bash
java Main
```

> Make sure `telemetry_dataset.csv` is in the **same folder** as the `.java` files before running.

---

## 🔄 How It Works

The program processes telemetry in a 5-step pipeline:

| Step | Class | What It Does |
|------|-------|--------------|
| 1 | `TelemetryLoader` | Reads the CSV; stores missing values as `Double.NaN` |
| 2 | `FlightAnalyzer` | Assigns a flight phase label to every row |
| 3 | `AnomalyDetector` | Scans all rows and flags anomalies with text notes |
| 4 | `AnomalyFixer` | Replaces bad altitude values using linear interpolation |
| 5 | `ReportGenerator` | Prints statistics, a data table, and a final summary |

---

## ✈️ Flight Phases Detected

The dataset contains a **two-segment mission**:

| Phase | Description |
|-------|-------------|
| `ASCENT` | Craft is climbing |
| `APOGEE_1` | Peak of the first flight segment (~915m) |
| `DESCENT` | Craft is falling |
| `LANDED` | Craft is on the ground between two launch segments |
| `APOGEE` | Global peak of the full mission (~1000m) |

---

## 🚨 Anomalies Detected

`AnomalyDetector` checks for five types of problems:

| # | Check | Trigger Condition |
|---|-------|-------------------|
| 1 | **Missing Altitude** | Empty cell in CSV (stored as `NaN`) |
| 2 | **Altitude Spike** | Change > 200m in one second (skipped at apogee rows) |
| 3 | **Wrong Direction** | Altitude drops during ascent, or rises during descent |
| 4 | **Battery Increase** | Voltage goes up — physically impossible mid-flight |
| 5 | **Temperature Outlier** | Z-score `> 2.5` standard deviations from mean |

---

## 🔧 Anomaly Fixes

`AnomalyFixer` repairs **altitude-only** issues using the midpoint formula:

```
fixedAltitude = (previousValidAltitude + nextValidAltitude) / 2
```

Fixed conditions:
- Missing altitude (`NaN`)
- Altitude spike vs previous row
- Altitude recorded as `0.0` during descent while clearly airborne

> ⚠️ Temperature and battery anomalies are **noted but not changed** — their values are physically plausible and altering them could introduce false data.  
> ⚠️ The `APOGEE` row is **never modified** — its high altitude is real.

---

## 📊 Sample Output

```
╔══════════════════════════════════════╗
║  SATELLITE TELEMETRY ANALYSIS SYSTEM ║
╚══════════════════════════════════════╝

✅ Loaded 96 rows from telemetry_dataset.csv

========================================
         FLIGHT PHASE SUMMARY
========================================
  Total data points   : 96
  ── Segment 1 ──────────────────────────
  Ascent              : rows 0 → 19
  First peak (APOGEE_1): row 20, t=21, alt=915.0m
  Descent             : rows 21 → 22
  Landed              : rows 23 → 52  (30 points on ground)
  ── Segment 2 ──────────────────────────
  Ascent              : rows 53 → 71
  APOGEE (main peak)  : row 72, t=73, alt=1000.0m
  Descent             : rows 73 → 95  (23 points)

========================================
         ANOMALY DETECTION REPORT
========================================
  ⚠️  t=30  | MISSING altitude value (empty in CSV)
  ...
  Total anomalies found: X

--- FINAL SUMMARY ---
  Total rows        : 96
  Rows with anomaly : X
  Rows auto-fixed   : X
  Rows still flagged: X

  ✅ Analysis complete.
```

---

## 📚 Concepts Covered

- Object-Oriented Design (model class, single-responsibility)
- File I/O with `BufferedReader`
- `ArrayList<T>` and generics
- `Double.NaN` for missing value handling
- Statistical methods: mean, standard deviation, z-score
- Linear interpolation
- In-place object mutation
- Formatted console output with `printf`

---

## 📄 License

Built for educational use by the Satellite Club. Free to use, modify, and extend.
