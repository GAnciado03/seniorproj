package videoapp.util;

import nu.pattern.OpenCV;

public class OpenCvHelpers {
    private OpenCvHelpers(){}

    public static void load() {
        OpenCV.loadLocally();
    }
}
