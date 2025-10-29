package videoapp.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Lightweight, vector Play/Pause icon with fixed dimensions to
 * avoid layout changes when toggling states.
 *
 * @author Glenn Anciado
 * @version 1.0
 */
public class PlayPauseIcon implements Icon {
    public enum Type { PLAY, PAUSE }

    private final int width;
    private final int height;
    private final Type type;
    private final Color color;

    public PlayPauseIcon(int width, int height, Type type) {
        this(width, height, type, new Color(230, 230, 230));
    }

    public PlayPauseIcon(int width, int height, Type type, Color color) {
        this.width = Math.max(8, width);
        this.height = Math.max(8, height);
        this.type = type == null ? Type.PLAY : type;
        this.color = color == null ? new Color(230, 230, 230) : color;
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

            if (type == Type.PLAY) {
                int[] xs = { pad, pad, pad + w };
                int[] ys = { pad, pad + h, pad + h / 2 };
                g2.fillPolygon(xs, ys, 3);
            } else {
                int barW = Math.max(2, w / 3);
                int gap = Math.max(2, w / 6);
                float arc = Math.max(2f, Math.min(barW, h) * 0.25f);
                Shape left = new RoundRectangle2D.Float(pad, pad, barW, h, arc, arc);
                Shape right = new RoundRectangle2D.Float(pad + barW + gap, pad, barW, h, arc, arc);
                g2.fill(left);
                g2.fill(right);
            }
        } finally {
            g2.dispose();
        }
    }

    @Override
    public int getIconWidth() { return width; }

    @Override
    public int getIconHeight() { return height; }
}

