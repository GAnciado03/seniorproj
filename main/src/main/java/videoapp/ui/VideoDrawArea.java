package videoapp.ui;

/**
 * Immutable value representing the rectangle where the current video frame is drawn.
 *
 * @author Glenn Anciado
 * @version 1.0
 */

public record VideoDrawArea(int x, int y, int width, int height) {}
