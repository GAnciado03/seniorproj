package videoapp.core;

/**
 * Abstraction for rendering video frames and UI messages. Implementations
 * decide how to display frames and react to lifecycle and progress events.
 *
 * @author Glenn Anciado
 * @version 1.0
 */

import java.awt.image.BufferedImage;

public interface VideoRenderer {
    void renderFrame(BufferedImage frame);
    void showMessage(String message);
    void onStopped();

    default void onProgress(long posMs, long durationMs) {}
    default void requestPackOnNextFrame() {}
}
