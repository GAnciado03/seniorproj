package videoapp.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;

/**
 * Simple fullscreen toggle icon. ENTER draws expand corners, EXIT draws collapse corners.
 *
 * @author Glenn Anciado
 * @version 1.0
 */

public class FullscreenIcon implements Icon {
    public enum Mode { ENTER, EXIT }

    private final int width;
    private final int height;
    private final Mode mode;
    private final Color color;

    public FullscreenIcon(int width, int height, Mode mode, Color color) {
        this.width = Math.max(8, width);
        this.height = Math.max(8, height);
        this.mode = mode == null ? Mode.ENTER : mode;
        this.color = color == null ? new Color(100, 100, 100) : color;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.translate(x, y);
            g2.setColor(color);

            int pad = Math.max(2, Math.min(width, height) / 6);
            int w = width - pad * 2;
            int h = height - pad * 2;
            int t = Math.max(2, Math.min(w, h) / 7); // stroke thickness
            int cx = pad + w / 2;
            int cy = pad + h / 2;
            int m = Math.min(w, h);
            int head = Math.max(3, m / 6);

            if (mode == Mode.ENTER) {
                // Outward arrows: from near center to corners
                drawArrow(g2, cx, cy, pad + w - 1, pad + 1, t, head);        // to top-right
                drawArrow(g2, cx, cy, pad + 1, pad + h - 1, t, head);        // to bottom-left
            } else {
                // Inward arrows: from corners toward center (like provided minimize image)
                drawArrow(g2, pad + w - 1, pad + 1, cx, cy, t, head);        // from top-right inward
                drawArrow(g2, pad + 1, pad + h - 1, cx, cy, t, head);        // from bottom-left inward
            }
        } finally {
            g2.dispose();
        }
    }

    private static void drawArrow(Graphics2D g2, int x1, int y1, int x2, int y2, int thickness, int headSize) {
        Stroke old = g2.getStroke();
        g2.setStroke(new BasicStroke(thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        double angle = Math.atan2(y2 - y1, x2 - x1);
        int hx = (int) Math.round(x2 - headSize * Math.cos(angle));
        int hy = (int) Math.round(y2 - headSize * Math.sin(angle));

        g2.drawLine(x1, y1, hx, hy);

        double sin = Math.sin(angle);
        double cos = Math.cos(angle);
        int wing = Math.max(2, headSize / 2);
        int xA = (int) Math.round(hx + (-sin) * wing);
        int yA = (int) Math.round(hy + (cos) * wing);
        int xB = (int) Math.round(hx - (-sin) * wing);
        int yB = (int) Math.round(hy - (cos) * wing);

        int[] xs = { x2, xA, xB };
        int[] ys = { y2, yA, yB };
        g2.fillPolygon(xs, ys, 3);

        g2.setStroke(old);
    }

    @Override
    public int getIconWidth() { return width; }

    @Override
    public int getIconHeight() { return height; }
}
