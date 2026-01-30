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
    public record State(Rectangle prevBounds, int prevExtendedState) {}

    public State enter(JFrame frame) {
        if (frame == null) return null;
        Rectangle bounds = frame.getBounds();
        int prevState = frame.getExtendedState();
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setVisible(true);
        return new State(bounds, prevState);
    }

    public void exit(JFrame frame, State state) {
        if (frame == null || state == null) return;
        int restoreState = normalizeState(state.prevExtendedState());
        frame.setExtendedState(JFrame.NORMAL);
        Rectangle bounds = state.prevBounds();
        if (bounds != null) {
            frame.setBounds(bounds);
        }
        frame.setExtendedState(restoreState);
        frame.setVisible(true);
    }

    public State toggle(JFrame frame, State current) {
        if (frame == null) return current;
        if (current != null) {
            exit(frame, current);
            return null;
        }
        return enter(frame);
    }

    private int normalizeState(int prevState) {
        int normalized = prevState & ~JFrame.ICONIFIED;
        return (normalized == 0) ? JFrame.NORMAL : normalized;
    }
}
