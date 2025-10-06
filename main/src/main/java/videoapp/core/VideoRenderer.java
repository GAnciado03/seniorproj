package videoapp.core;

import java.awt.image.BufferedImage;

public interface VideoRenderer {
    void renderFrame(BufferedImage frame);
    void showMessage(String message);
    void onStopped();

    default void onProgress(long posMs, long durationMs) {}
    default void requestPackOnNextFrame() {}
}
