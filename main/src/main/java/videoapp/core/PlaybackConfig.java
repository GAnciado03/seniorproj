package videoapp.core;

/**
 * Mutable playback configuration container including speed and target width/height.
 * Shared between the player and renderer pipeline.
 *
 * @author Glenn Anciado
 * @version 1.0
 */

public class PlaybackConfig {
    public volatile double speed = 1.0;
    public volatile int targetWidth = 0;
    public volatile int targetHeight = 0;
}
