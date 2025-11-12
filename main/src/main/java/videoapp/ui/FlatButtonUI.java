package videoapp.ui;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;

/**
 * Lightweight button UI that simply fills the component with its background
 * color so dark-mode palettes render correctly regardless of platform LAF.
 *
 * @author Glenn Anciado
 * @version 1.0
 */

 public final class FlatButtonUI extends BasicButtonUI {
    private static final FlatButtonUI INSTANCE = new FlatButtonUI();

    public static FlatButtonUI get() {
        return INSTANCE;
    }

    private FlatButtonUI() {}

    @Override
    public void installDefaults(AbstractButton b) {
        super.installDefaults(b);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setFocusable(false);
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        AbstractButton button = (AbstractButton) c;
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = button.getBackground();
            if (!button.isEnabled()) {
                fill = fill.brighter();
            } else if (button.getModel().isPressed()) {
                fill = fill.darker();
            }
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 8, 8);
        } finally {
            g2.dispose();
        }
        super.paint(g, c);
    }
}
