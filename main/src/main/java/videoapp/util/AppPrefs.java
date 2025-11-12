package videoapp.util;

import java.io.File;
import java.util.prefs.Preferences;

/**
 * Simple singleton wrapper around {@link java.util.prefs.Preferences}
 * with helpers for remembering and retrieving last-used directories.
 *
 * @author Glenn Anciado
 * @version 1.0
 */

public final class AppPrefs {
    public static final class Keys {
        public static final String LAST_VIDEO_DIR = "lastVideoDir";
        public static final String LAST_CSV_DIR   = "lastCsvDir";
        public static final String DARK_MODE      = "darkMode";
        private Keys() {}
    }

    private static final AppPrefs INSTANCE = new AppPrefs();
    public static AppPrefs get() { return INSTANCE; }
    private final Preferences prefs;

    private AppPrefs() {
        prefs = Preferences.userNodeForPackage(AppPrefs.class);
    }

    public File getLastDir(String key) {
        try {
            String path = prefs.get(key, null);
            if (path == null) return null;
            File f = new File(path);
            return (f.isDirectory()) ? f : null;
        } catch (Exception ignore) { return null; }
    }

    public void rememberDir(String key, File selected) {
        try {
            if (selected == null) return;
            File dir = selected.isDirectory() ? selected : selected.getParentFile();
            if (dir != null && dir.isDirectory()) {
                prefs.put(key, dir.getAbsolutePath());
            }
        } catch (Exception ignore) { }
    }

    public boolean isDarkMode() {
        try {
            return prefs.getBoolean(Keys.DARK_MODE, false);
        } catch (Exception ignore) {
            return false;
        }
    }

    public void setDarkMode(boolean enabled) {
        try {
            prefs.putBoolean(Keys.DARK_MODE, enabled);
        } catch (Exception ignore) { }
    }
}
