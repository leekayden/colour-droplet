# Colour Droplet 💧

A lightweight, modern Java Swing desktop colour picker with a sleek dark theme.

[![Build](https://github.com/leekayden/colour-droplet/actions/workflows/ci.yml/badge.svg)](https://github.com/leekayden/colour-droplet/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java 8+](https://img.shields.io/badge/Java-8%2B-orange.svg)](https://www.oracle.com/java/)
[![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20macOS%20%7C%20Linux-lightgrey.svg)](#run)

Click the droplet button, move the crosshair magnifier anywhere on your screen (even across multi-monitor setups), and click to sample the pixel colour under your cursor. **Colour Droplet** instantly displays the colour's Hex, RGB, and HSL values, visualizes it across hue and tint/shade scales, and can automatically copy values to your clipboard.

---

## ✨ Features

- **Screen-wide Crosshair Magnifier**: Zoom in on any pixel on any connected display to pick precise shades.
- **Multiple Colour Formats**:
  - `#RRGGBB` hex code
  - `R/G/B` formatted as `rgb(r, g, b)`
  - `H/S/L` formatted as `hsl(h, s%, l%)`
  - One-click copy buttons for every format.
- **Visual Colour Scales**:
  - **Hue Scale (0°–360°)**: Highlights where the picked colour sits on the rainbow spectrum.
  - **Tint → Shade Scale**: Shows lightness progression from pure white to full colour to black.
- **History Palette**: Stores recent colour picks for fast reference and re-selection.
- **Auto-Copy to Clipboard**: Option to automatically put `#RRGGBB` hex into your clipboard upon sampling.
- **Dark Modern UI**: Styled with [FlatLaf](https://www.formdev.com/flatlaf/) for a native dark look and feel across platforms.
- **Zero Heavy Dependencies**: Self-contained, tiny footprint, fast launch time.

---

## 🚀 Getting Started

### Prerequisites

You only need a Java Development Kit (JDK 8 or newer) with `javac` available in your system `PATH`.

To verify your installation:
```bash
javac -version
java -version
```

### Launching the Application

Clone this repository:
```bash
git clone https://github.com/leekayden/colour-droplet.git
cd colour-droplet
```

- **Windows**:
  Double-click `run.bat` or run:
  ```cmd
  run.bat
  ```

- **macOS / Linux**:
  Make the script executable and run:
  ```bash
  chmod +x run.sh
  ./run.sh
  ```

#### Manual Compilation & Run

If you prefer to compile and launch directly:

**Windows:**
```cmd
javac -encoding UTF-8 -cp "lib/*" -d out src/*.java
java -cp "out;lib/*" ColorDropletApp
```

**macOS / Linux:**
```bash
javac -encoding UTF-8 -cp "lib/*" -d out src/*.java
java -cp "out:lib/*" ColorDropletApp
```

---

## 🎯 How to Use

1. Click **Pick a colour** (or press <kbd>Alt</kbd> + <kbd>P</kbd>).
   - The application window hides and a full-screen magnifier overlay tracks your cursor.
   - The magnifier displays a zoomed pixel grid along with the exact hex & RGB values in real time.
2. **Left-click** anywhere to capture that pixel.
   - Press <kbd>Esc</kbd> or **right-click** at any time to cancel without picking.
3. The captured colour is displayed on the main dashboard:
   - Click the large color swatch to copy its Hex code immediately.
   - Click the individual copy icons next to RGB or HSL to copy CSS-ready values (`rgb(...)`, `hsl(...)`).
   - Observe the marker positions on the **Hue** and **Lightness** scales.
4. Access previous picks in the **Recent** swatches strip at the bottom.
5. Check **"Copy hex to clipboard when picking"** to save clicks during active design sessions.

---

## 🔧 Troubleshooting & Display Scaling

- **Multi-Monitor Setups**: Picking works across all connected monitors and virtual screen coordinates.
- **Restricted Screens**: Elevated windows (such as Windows UAC prompts or macOS secure entry dialogs) cannot be sampled due to OS security restrictions.
- **High-DPI Scaling on Windows**:
  On some Windows displays scaled above 100%, certain JDK distributions might cause cursor coordinates to slightly offset. If you notice an alignment shift, launch the application with DPI scaling disabled:
  ```cmd
  java -Dsun.java2d.uiScale=1 -cp "out;lib/*" ColorDropletApp
  ```

---

## 📂 Project Structure

```
colour-droplet/
├── .github/
│   ├── ISSUE_TEMPLATE/
│   │   ├── bug_report.md
│   │   └── feature_request.md
│   ├── workflows/
│   │   └── ci.yml
│   └── pull_request_template.md
├── lib/
│   └── flatlaf-3.7.2.jar     # Bundled FlatLaf Look and Feel (MIT)
├── src/
│   ├── ColorDropletApp.java  # Main application window, UI, and event dispatch
│   ├── ColorMath.java        # Colour space conversions (Hex, RGB, HSL)
│   ├── ColorScalePanel.java  # Custom Swing painted hue and shade scales
│   └── PickerOverlay.java    # Screen capture overlay and magnifier window
├── .gitignore
├── CODE_OF_CONDUCT.md
├── CONTRIBUTING.md
├── LICENSE
├── README.md
├── run.bat                   # Quick start launcher for Windows
└── run.sh                    # Quick start launcher for macOS/Linux
```

---

## 🤝 Contributing

Contributions are welcome! Please check out [CONTRIBUTING.md](CONTRIBUTING.md) to get started and review our [Code of Conduct](CODE_OF_CONDUCT.md).

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).

FlatLaf is licensed under the [Apache License 2.0 / MIT License](https://www.formdev.com/flatlaf/).
