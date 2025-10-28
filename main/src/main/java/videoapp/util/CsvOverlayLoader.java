package videoapp.util;

import videoapp.ui.OverlayPoint;
import videoapp.ui.TimedOverlayPoint;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Minimal CSV reader for gaze/point overlays. Looks for columns named
 * "surfaceX" and "surfaceY" (normalized 0..1). If not found, tries
 * to read the first two numeric columns as x,y normalized values.
 *
 * Stateless utility, not a singleton. All methods are static and the
 * constructor is private to prevent instantiation.
 *
 * @author Glenn Anciado
 * @version 1.0
 */

public final class CsvOverlayLoader {
    private CsvOverlayLoader() {}

    public static List<OverlayPoint> load(File csv) {
        List<OverlayPoint> out = new ArrayList<>();
        if (csv == null || !csv.isFile()) return out;
        try (BufferedReader br = new BufferedReader(new FileReader(csv, StandardCharsets.UTF_8))) {
            String header = br.readLine();
            if (header == null) return out;
            String[] cols = splitCsv(header);

            int xIdx = -1, yIdx = -1;
            for (int i = 0; i < cols.length; i++) {
                String name = cols[i].trim().toLowerCase(Locale.ROOT);
                if (name.equals("surfacex")) xIdx = i;
                else if (name.equals("surfacey")) yIdx = i;
            }

            String line;
            while ((line = br.readLine()) != null) {
                if (line.isEmpty()) continue;
                String[] parts = splitCsv(line);
                try {
                    double x, y;
                    if (xIdx >= 0 && yIdx >= 0 && xIdx < parts.length && yIdx < parts.length) {
                        x = parse(parts[xIdx]);
                        y = parse(parts[yIdx]);
                    } else {
                        int found = 0;
                        double[] tmp = new double[2];
                        for (String p : parts) {
                            try {
                                double v = parse(p);
                                tmp[found++] = v;
                                if (found == 2) break;
                            } catch (Exception ignore) {
                            }
                        }
                        if (found < 2) continue;
                        x = tmp[0];
                        y = tmp[1];
                    }

                    // Clamp to [0,1] if values appear normalized; if they look like pixels, skip
                    if (Double.isFinite(x) && Double.isFinite(y)) {
                        if (x >= 0.0 && x <= 1.0 && y >= 0.0 && y <= 1.0) {
                            out.add(new OverlayPoint(x, y));
                        }
                    }
                } catch (Exception ignore) {
                }
            }
        } catch (Exception e) {
            // swallow; return what we have
        }
        return out;
    }


    public static List<TimedOverlayPoint> loadTimed(File csv) {
        List<TimedOverlayPoint> out = new ArrayList<>();
        if (csv == null || !csv.isFile()) return out;
        try (BufferedReader br = new BufferedReader(new FileReader(csv, StandardCharsets.UTF_8))) {
            String header = br.readLine();
            if (header == null) return out;
            String[] cols = splitCsv(header);

            int xIdx = -1, yIdx = -1, tIdx = -1;
            for (int i = 0; i < cols.length; i++) {
                String name = cols[i].trim().toLowerCase(Locale.ROOT);
                if (name.equals("surfacex")) xIdx = i;
                else if (name.equals("surfacey")) yIdx = i;
                else if (name.equals("devicetimestamp") || name.equals("localtimestamp") ||
                         name.equals("timestamp") || name.equals("time") || name.equals("t")) {
                    if (tIdx < 0) tIdx = i;
                }
            }

            if (tIdx < 0) return out; // no time column

            String line;
            Double firstTimeSec = null;
            while ((line = br.readLine()) != null) {
                if (line.isEmpty()) continue;
                String[] parts = splitCsv(line);
                if (tIdx >= parts.length) continue;
                try {
                    double tSec = parse(parts[tIdx]);
                    if (firstTimeSec == null) firstTimeSec = tSec;
                    double relSec = tSec - firstTimeSec;

                    double x, y;
                    if (xIdx >= 0 && yIdx >= 0 && xIdx < parts.length && yIdx < parts.length) {
                        x = parse(parts[xIdx]);
                        y = parse(parts[yIdx]);
                    } else {
                        // fallback: first two numeric-looking fields (besides time column)
                        int found = 0;
                        double[] tmp = new double[2];
                        for (int i = 0; i < parts.length; i++) {
                            if (i == tIdx) continue;
                            try {
                                double v = parse(parts[i]);
                                tmp[found++] = v;
                                if (found == 2) break;
                            } catch (Exception ignore) {}
                        }
                        if (found < 2) continue;
                        x = tmp[0];
                        y = tmp[1];
                    }

                    if (Double.isFinite(x) && Double.isFinite(y) &&
                        x >= 0.0 && x <= 1.0 && y >= 0.0 && y <= 1.0 &&
                        Double.isFinite(relSec) && relSec >= -3600 && relSec <= 1e8) {
                        long ms = (long) Math.round(relSec * 1000.0);
                        out.add(new TimedOverlayPoint(x, y, ms));
                    }
                } catch (Exception ignore) {
                }
            }
        } catch (Exception e) {
        }
        return out;
    }

    private static double parse(String s) {
        s = s.trim();
        if (s.isEmpty()) throw new IllegalArgumentException("empty");
        return Double.parseDouble(s);
    }

    // Minimal CSV split supporting simple quoted fields and commas
    private static String[] splitCsv(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        sb.append('"');
                        i++; // escaped quote
                    } else {
                        inQuotes = false;
                    }
                } else {
                    sb.append(c);
                }
            } else {
                if (c == ',') {
                    fields.add(sb.toString());
                    sb.setLength(0);
                } else if (c == '"') {
                    inQuotes = true;
                } else {
                    sb.append(c);
                }
            }
        }
        fields.add(sb.toString());
        return fields.toArray(new String[0]);
    }
}
