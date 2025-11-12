package videoapp.util;

import java.io.File;
import java.util.prefs.Preferences;

/**
 * Persists per-CSV overlay time offsets so datasets without frame indices
 * can be aligned once and auto-synced on subsequent loads.
 *
 * @author Glenn Anciado
 * @version 1.0
 */

public final class OverlayOffsetStore {
    private static final Preferences PREFS = Preferences.userNodeForPackage(OverlayOffsetStore.class);
    private OverlayOffsetStore() {}

    private static String keyFor(File csv) {
        if (csv == null) return null;
        String path = csv.getAbsolutePath();
        int h = path.hashCode();
        return "offset." + Integer.toHexString(h);
    }

    public static Long get(File csv) {
        try {
            String key = keyFor(csv);
            if (key == null) return null;
            String v = PREFS.get(key, null);
            return (v != null) ? Long.parseLong(v) : null;
        } catch (Exception ignore) { return null; }
    }

    public static void put(File csv, long offsetMs) {
        try {
            String key = keyFor(csv);
            if (key == null) return;
            PREFS.put(key, Long.toString(offsetMs));
        } catch (Exception ignore) {}
    }
}

