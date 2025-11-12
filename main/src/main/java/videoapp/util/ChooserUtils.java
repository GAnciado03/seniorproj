package videoapp.util;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

/** DRY helpers for initializing JFileChoosers with sensible defaults.
 *
 * @author Glenn Anciado
 * @version 2.0
 */

public final class ChooserUtils {
    private ChooserUtils() {}

    public static File initialVideoDir() {
        File last = AppPrefs.get().getLastDir(AppPrefs.Keys.LAST_VIDEO_DIR);
        File proj = ProjectPaths.repoRoot();
        File vids = ProjectPaths.sampleVideosDir();
        File legacy = ProjectPaths.legacyRepoRoot();
        return ProjectPaths.firstExisting(last, vids, proj, legacy);
    }

    public static File initialCsvDir() {
        File last = AppPrefs.get().getLastDir(AppPrefs.Keys.LAST_CSV_DIR);
        File data = ProjectPaths.testDataDir();
        File proj = ProjectPaths.repoRoot();
        File legacy = ProjectPaths.legacyTestDataDir();
        return ProjectPaths.firstExisting(last, data, proj, legacy);
    }

    public static void rememberVideoSelection(File f) {
        AppPrefs.get().rememberDir(AppPrefs.Keys.LAST_VIDEO_DIR, f);
    }

    public static void rememberCsvSelection(File f) {
        AppPrefs.get().rememberDir(AppPrefs.Keys.LAST_CSV_DIR, f);
    }

    public static void applyCsvFilter(JFileChooser chooser) {
        chooser.setFileFilter(new FileNameExtensionFilter("CSV files", "csv"));
    }
}

