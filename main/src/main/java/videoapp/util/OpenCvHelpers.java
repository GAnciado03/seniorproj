package videoapp.util;

/**
 * Small helper to load the OpenCV native library using the nu.pattern loader.
 *
 * @author Glenn Anciado
 * @version 1.0
 */

import nu.pattern.OpenCV;

public class OpenCvHelpers {
    private OpenCvHelpers(){}

    public static void load() {
        OpenCV.loadLocally();
    }
}
