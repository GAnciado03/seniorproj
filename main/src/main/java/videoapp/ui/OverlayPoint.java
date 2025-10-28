package videoapp.ui;

/**
 * Simple immutable value type representing a normalized overlay point
 * location on the video surface. Coordinates are in the range
 * [0,1] relative to video width/height.
 *
 * Not a singleton: many instances are expected (one per CSV row).
 *
 * @author Glenn Anciado
 * @version 1.0
 */

public class OverlayPoint {
    public final double xNorm;
    public final double yNorm;

    public OverlayPoint(double xNorm, double yNorm) {
        this.xNorm = xNorm;
        this.yNorm = yNorm;
    }
}
