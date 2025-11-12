package videoapp.util;

import videoapp.ui.OverlayPoint;
import videoapp.ui.TimedOverlayPoint;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Loads overlay points from CSV files while keeping parsing logic isolated from UI concerns.
 * The loader is instantiable so callers can inject their own instance instead of relying on globals.
 *
 * @author Glenn Anciado
 * @version 2.0
 */

public class CsvOverlayLoader {

    public List<OverlayPoint> load(File csv) {
        List<OverlayPoint> points = new ArrayList<>();
        if (!isReadable(csv)) {
            return points;
        }

        try (BufferedReader br = reader(csv)) {
            String[] header = readHeader(br);
            if (header == null) {
                return points;
            }

            OverlayColumns columns = detectOverlayColumns(header, false);
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }
                String[] parts = splitCsv(line);
                double[] xy = extractCoordinates(parts, columns);
                if (xy == null) {
                    continue;
                }
                double x = xy[0];
                double y = xy[1];
                if (isValidNorm(x) && isValidNorm(y)) {
                    points.add(new OverlayPoint(x, y));
                }
            }
        } catch (Exception ignore) {
        }
        return points;
    }

    public List<TimedOverlayPoint> loadTimed(File csv) {
        List<TimedOverlayPoint> points = new ArrayList<>();
        if (!isReadable(csv)) {
            return points;
        }

        try (BufferedReader br = reader(csv)) {
            String[] header = readHeader(br);
            if (header == null) {
                return points;
            }

            OverlayColumns columns = detectOverlayColumns(header, true);
            if (!columns.hasTime()) {
                return points;
            }

            String line;
            Double firstTimeSec = null;
            while ((line = br.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }
                String[] parts = splitCsv(line);
                if (columns.timeIdx() >= parts.length) {
                    continue;
                }
                try {
                    double tSec = parse(parts[columns.timeIdx()]);
                    if (firstTimeSec == null) {
                        firstTimeSec = tSec;
                    }
                    double relSec = tSec - firstTimeSec;
                    if (!Double.isFinite(relSec) || relSec < -3600 || relSec > 1e8) {
                        continue;
                    }

                    double[] xy = extractCoordinates(parts, columns);
                    if (xy == null) {
                        continue;
                    }
                    double x = xy[0];
                    double y = xy[1];
                    if (!isValidNorm(x) || !isValidNorm(y)) {
                        continue;
                    }
                    long ms = Math.round(relSec * 1000.0);
                    points.add(new TimedOverlayPoint(x, y, ms));
                } catch (Exception ignore) {
                }
            }
        } catch (Exception ignore) {
        }
        return points;
    }

    public Long suggestOffsetFromFrameIndex(File csv, double fps) {
        if (!isReadable(csv) || !(fps > 0 && Double.isFinite(fps))) {
            return null;
        }

        try (BufferedReader br = reader(csv)) {
            String[] header = readHeader(br);
            if (header == null) {
                return null;
            }

            FrameColumns columns = detectFrameColumns(header);
            if (!columns.valid()) {
                return null;
            }

            List<Long> offsets = new ArrayList<>();
            String line;
            Double firstTimeSec = null;
            int rows = 0;
            while ((line = br.readLine()) != null && rows < 500) {
                if (line.isEmpty()) {
                    continue;
                }
                String[] parts = splitCsv(line);
                if (!columns.inBounds(parts.length)) {
                    continue;
                }
                try {
                    double tSec = parse(parts[columns.timeIdx()]);
                    if (firstTimeSec == null) {
                        firstTimeSec = tSec;
                    }
                    double relSec = tSec - firstTimeSec;
                    if (!Double.isFinite(relSec)) {
                        continue;
                    }
                    long csvMs = Math.round(relSec * 1000.0);
                    csvMs += columns.durationAdjustment(parts);
                    long frameIndex = Math.round(parse(parts[columns.frameIdx()]));
                    long expectedMs = Math.round((frameIndex * 1000.0) / fps);
                    offsets.add(expectedMs - csvMs);
                    rows++;
                } catch (Exception ignore) {
                }
            }

            if (offsets.isEmpty()) {
                return null;
            }
            Collections.sort(offsets);
            return offsets.get(offsets.size() / 2);
        } catch (Exception ignore) {
            return null;
        }
    }

    private boolean isReadable(File csv) {
        return csv != null && csv.isFile();
    }

    private BufferedReader reader(File csv) throws IOException {
        return new BufferedReader(new FileReader(csv, StandardCharsets.UTF_8));
    }

    private String[] readHeader(BufferedReader br) throws IOException {
        String header = br.readLine();
        return (header == null) ? null : splitCsv(header);
    }

    private OverlayColumns detectOverlayColumns(String[] cols, boolean includeTime) {
        int xIdx = -1;
        int yIdx = -1;
        int timeIdx = -1;
        for (int i = 0; i < cols.length; i++) {
            String name = cols[i];
            if (isXName(name)) {
                xIdx = i;
            } else if (isYName(name)) {
                yIdx = i;
            } else if (includeTime && timeIdx < 0 && isTimeName(name)) {
                timeIdx = i;
            }
        }
        return new OverlayColumns(xIdx, yIdx, includeTime ? timeIdx : -1);
    }

    private FrameColumns detectFrameColumns(String[] cols) {
        int frameIdx = -1;
        int timeIdx = -1;
        int durationIdx = -1;
        for (int i = 0; i < cols.length; i++) {
            String name = cols[i].trim().toLowerCase(Locale.ROOT);
            if (frameIdx < 0 && (name.equals("start_frame_index") || name.equals("frame") ||
                    name.equals("start_frame") || name.equals("frame_index"))) {
                frameIdx = i;
            } else if (durationIdx < 0 && (name.equals("duration") || name.equals("duration_ms"))) {
                durationIdx = i;
            } else if (timeIdx < 0 && isTimeName(name)) {
                timeIdx = i;
            }
        }
        return new FrameColumns(frameIdx, timeIdx, durationIdx);
    }

    private double[] extractCoordinates(String[] parts, OverlayColumns columns) {
        if (columns.xIdx() >= 0 && columns.yIdx() >= 0 &&
                columns.xIdx() < parts.length && columns.yIdx() < parts.length) {
            try {
                return new double[]{parse(parts[columns.xIdx()]), parse(parts[columns.yIdx()])};
            } catch (Exception ignore) {
                return null;
            }
        }
        return findFirstTwoNumbers(parts, columns.timeIdx());
    }

    private double[] findFirstTwoNumbers(String[] parts, int skipIndex) {
        double[] tmp = new double[2];
        int found = 0;
        for (int i = 0; i < parts.length && found < 2; i++) {
            if (i == skipIndex) {
                continue;
            }
            try {
                tmp[found++] = parse(parts[i]);
            } catch (Exception ignore) {
            }
        }
        return (found == 2) ? tmp : null;
    }

    private boolean isValidNorm(double value) {
        return Double.isFinite(value) && value >= 0.0 && value <= 1.0;
    }

    private boolean isXName(String name) {
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("surfacex") || normalized.equals("norm_pos_x");
    }

    private boolean isYName(String name) {
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("surfacey") || normalized.equals("norm_pos_y");
    }

    private boolean isTimeName(String name) {
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("devicetimestamp") || normalized.equals("localtimestamp") ||
                normalized.equals("timestamp") || normalized.equals("time") || normalized.equals("t") ||
                normalized.equals("start_timestamp") || normalized.contains("timestamp");
    }

    private double parse(String s) {
        String trimmed = s.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("empty");
        }
        return Double.parseDouble(trimmed);
    }

    private String[] splitCsv(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        sb.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    sb.append(c);
                }
            } else if (c == ',') {
                fields.add(sb.toString());
                sb.setLength(0);
            } else if (c == '"') {
                inQuotes = true;
            } else {
                sb.append(c);
            }
        }
        fields.add(sb.toString());
        return fields.toArray(new String[0]);
    }

    private record OverlayColumns(int xIdx, int yIdx, int timeIdx) {
        boolean hasTime() {
            return timeIdx >= 0;
        }
    }

    private record FrameColumns(int frameIdx, int timeIdx, int durationIdx) {
        boolean valid() {
            return frameIdx >= 0 && timeIdx >= 0;
        }

        boolean inBounds(int length) {
            return frameIdx >= 0 && timeIdx >= 0 && frameIdx < length && timeIdx < length;
        }

        long durationAdjustment(String[] parts) {
            if (durationIdx >= 0 && durationIdx < parts.length) {
                try {
                    double dur = Double.parseDouble(parts[durationIdx].trim());
                    if (dur > 1000) {
                        dur /= 1000.0;
                    }
                    return Math.round((dur * 1000.0) / 2.0);
                } catch (Exception ignore) {
                    return 0L;
                }
            }
            return 0L;
        }
    }
}
