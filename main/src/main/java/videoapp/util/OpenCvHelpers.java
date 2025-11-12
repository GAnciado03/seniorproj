package videoapp.util;

import nu.pattern.OpenCV;
/**
 * Small helper to load the OpenCV native library using the nu.pattern loader.
 *
 * @author Glenn Anciado
 * @version 1.0
 */

public class OpenCvHelpers {
    private OpenCvHelpers(){}

    public static void load() {
        OpenCV.loadLocally();
    }
}
