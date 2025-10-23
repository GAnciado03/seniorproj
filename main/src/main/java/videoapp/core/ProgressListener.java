package videoapp.core;

/**
 * Listener interface for playback progress updates, receiving the current
 * position and total duration in milliseconds.
 *
 * @author Glenn Anciado
 * @version 1.0
 */

public interface ProgressListener {
    void onProgress(long posMs, long durationMs);
}
