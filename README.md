# Visualization of Emotions

Name: Glenn Anciado
Professor: Javier Gonzales Sanchez
Description: Desktop Java/Swing video player that overlays gaze points from CSV files, provides heatmap summaries with features like dark mode, fullscreen controls, and CSV/video loading workflows.

## Build & Run

1. Install Java 21+ (project currently targets Java 24).
2. From the `main/` directory run:

   ```bash
   cd main
   mvn -DargLine="--enable-native-access=ALL-UNNAMED" exec:java
   ```

   The flag is required because OpenCV uses restricted native access on newer JDKs.

## Usage

1. Launch the program and press the button "Load CSV & Video" to select data. The app prompts for the CSV first, then the video.
2. The player renders the video, overlays numbered gaze circles, and shows a heatmap summary at the end of playback.
3. Use the gear icon for playback speed, resolution, and dark mode toggles; use the fullscreen icon to maximize/minimize.

## Repository Notes

- `jgs-testing-data/` contain local-only test assets and are ignored via `.gitignore`.

## Attributions

Settings icon: "Settings icons" created by Freepik — from Flaticon
https://www.flaticon.com/free-icons/settings
