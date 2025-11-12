package videoapp.util;

import java.io.File;

/** Utility methods for project-relative directories (DRY helper).
 *
 * @author Glenn Anciado
 * @version 1.0
 */

public final class ProjectPaths {
    private ProjectPaths() {}

    public static File repoRoot() {
        return new File(System.getProperty("user.dir"));
    }

    public static File sampleVideosDir() {
        return new File(repoRoot(), "jgs-VideoPlayer/src/main/resources");
    }

    public static File testDataDir() {
        return new File(repoRoot(), "jgs-testing-data");
    }

    public static File legacyRepoRoot() {
        return new File(new File(new File(new File(System.getProperty("user.home"), "OneDrive"),
                "Documents"), "GitHub"), "seniorproj");
    }

    public static File legacyTestDataDir() {
        return new File(legacyRepoRoot(), "jgs-testing-data");
    }

    public static File firstExisting(File... candidates) {
        if (candidates == null) return null;
        for (File f : candidates) {
            if (f != null && f.isDirectory()) return f;
        }
        return null;
    }
}

