package videoapp.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.AlphaComposite;

/**
 * Simple vector gear icon for settings.
 *
 * @author Glenn Anciado
 * @version 2.0
 */

public class GearIcon implements Icon {
    private final int width;
    private final int height;
    private final Color color;

    public GearIcon(int width, int height, Color color) {
        this.width = Math.max(8, width);
        this.height = Math.max(8, height);
        this.color = color == null ? new Color(100, 100, 100) : color;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.translate(x, y);
            g2.setColor(color);

            int pad = Math.max(1, Math.min(width, height) / 8);
            int w = width - pad * 2;
            int h = height - pad * 2;
            int r = Math.min(w, h) / 2;
            int cx = pad + w / 2;
            int cy = pad + h / 2;

            int teeth = 8;
            int toothW = Math.max(2, r / 3);
            int toothL = Math.max(3, r / 2);
            for (int i = 0; i < teeth; i++) {
                double angle = (Math.PI * 2 * i) / teeth;
                AffineTransform old = g2.getTransform();
                g2.translate(cx, cy);
                g2.rotate(angle);
                g2.fillRoundRect(r - toothL, -toothW / 2, toothL, toothW, toothW, toothW);
                g2.setTransform(old);
            }

            g2.fillOval(cx - r + toothW / 3, cy - r + toothW / 3, (r - toothW / 3) * 2, (r - toothW / 3) * 2);

            g2.setComposite(AlphaComposite.Clear);
            int holeR = Math.max(2, r / 2);
            Shape hole = new Ellipse2D.Float(cx - holeR, cy - holeR, holeR * 2f, holeR * 2f);
            g2.fill(hole);
            g2.setComposite(AlphaComposite.SrcOver);
        } finally {
            g2.dispose();
        }
    }

    @Override
    public int getIconWidth() { return width; }

    @Override
    public int getIconHeight() { return height; }
}
