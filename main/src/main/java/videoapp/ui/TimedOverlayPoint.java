package videoapp.ui;

/**
 * Immutable overlay point with an associated timestamp in milliseconds
 * relative to the start of the video (t=0).
 *
 * Not a singleton: many instances are expected (one per timed CSV row).
 *
 * @author Glenn Anciado
 * @version 1.0
 */

public class TimedOverlayPoint extends OverlayPoint {
    public final long timeMs;

    public TimedOverlayPoint(double xNorm, double yNorm, long timeMs) {
        super(xNorm, yNorm);
        this.timeMs = timeMs;
    }
}
