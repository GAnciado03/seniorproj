package videoapp.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;

/**
 * Simple spinner overlay that can be painted on top of the video surface
 * while the player is busy seeking.
 *
 * @author Glenn Anciado
 * @version 1.0
 */

public final class LoadingOverlay {
    private final Runnable repaintCallback;
    private final Timer timer;
    private volatile boolean active = false;
    private volatile int angle = 0;

    public LoadingOverlay(Runnable repaintCallback) {
        this.repaintCallback = (repaintCallback != null) ? repaintCallback : () -> {};
        this.timer = new Timer(90, e -> {
            angle = (angle + 12) % 360;
            this.repaintCallback.run();
        });
        this.timer.setRepeats(true);
    }

    public void show() {
        if (SwingUtilities.isEventDispatchThread()) {
            startSpinner();
        } else {
            SwingUtilities.invokeLater(this::startSpinner);
        }
    }

    public void hide() {
        if (SwingUtilities.isEventDispatchThread()) {
            stopSpinner();
        } else {
            SwingUtilities.invokeLater(this::stopSpinner);
        }
    }

    public boolean isActive() {
        return active;
    }

    public void paint(Graphics2D g, Rectangle area) {
        if (!active || area == null) {
            return;
        }
        int size = Math.min(area.width, area.height) / 6;
        size = Math.max(size, 42);
        int cx = area.x + area.width / 2;
        int cy = area.y + area.height / 2;
        int half = size / 2;
        Shape arc = new Arc2D.Double(cx - half, cy - half, size, size, angle, 270, Arc2D.OPEN);
        Object prevAA = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Stroke prevStroke = g.getStroke();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0, 0, 0, 90));
        g.fillRoundRect(cx - size, cy - size, size * 2, size * 2, size / 2, size / 2);
        g.setColor(new Color(255, 255, 255, 240));
        g.setStroke(new BasicStroke(Math.max(4f, size / 12f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(arc);
        g.setStroke(prevStroke);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, prevAA);
    }

    private void startSpinner() {
        if (active) {
            return;
        }
        active = true;
        angle = 0;
        timer.start();
        repaintCallback.run();
    }

    private void stopSpinner() {
        if (!active) {
            return;
        }
        active = false;
        timer.stop();
        repaintCallback.run();
    }
}
