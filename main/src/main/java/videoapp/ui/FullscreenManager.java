package videoapp.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Utility for entering, exiting, and toggling fullscreen mode on a
 * JFrame, preserving and restoring prior window state.
 *
 * @author Glenn Anciado
 * @version 1.0
 */

public class FullscreenManager {
    public record State(Rectangle prevBounds, boolean prevDecorated, GraphicsDevice device) {}

    public State enter(JFrame frame) {
        if(frame == null) return null;
        GraphicsDevice gd = frame.getGraphicsConfiguration().getDevice();
        Rectangle bounds = frame.getBounds();
        boolean decorated = frame.isUndecorated();

        frame.dispose();
        frame.setUndecorated(true);
        frame.setVisible(true);
        gd.setFullScreenWindow(frame);

        return new State(bounds, decorated, gd);
    }

    public void exit(JFrame frame, State state) {
        if (frame == null || state == null) return;

        if(state.device() != null) {
            state.device().setFullScreenWindow(null);
        }
        frame.dispose();
        frame.setUndecorated(state.prevDecorated());
        frame.setBounds(state.prevBounds() != null
                ? state.prevBounds()
                : new Rectangle(100, 100, 600, 400));
        frame.setVisible(true);
    }
    public State toggle(JFrame frame, State current) {
        if(frame == null) return current;
        GraphicsDevice gd = frame.getGraphicsConfiguration().getDevice();
        boolean isFullscreen = gd.getFullScreenWindow() == frame;

        if(isFullscreen) {
            exit(frame, current);
            return null;
        } else {
            return enter(frame);
        }
    }
}
