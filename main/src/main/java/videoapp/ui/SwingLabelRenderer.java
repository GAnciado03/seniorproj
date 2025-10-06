package videoapp.ui;

import videoapp.core.VideoRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class SwingLabelRenderer implements VideoRenderer{
    private final JLabel label;
    private volatile boolean packNextFrame = true;

    public SwingLabelRenderer(JLabel label) {
        this.label = label;
    }

    @Override
    public void renderFrame(BufferedImage frame) {
        if(packNextFrame) {
            packNextFrame = false;
            Dimension d = new Dimension(frame.getWidth(), frame.getHeight());
            SwingUtilities.invokeLater(() -> {
                label.setPreferredSize(d);
                label.setMinimumSize(null);
                label.setMaximumSize(null);
                label.setText(null);
                Window w = SwingUtilities.getWindowAncestor(label);
                if(w != null) {
                    w.pack();
                }
            });
        }
        SwingUtilities.invokeLater(() -> {
            label.setText(null);
            label.setIcon(new ImageIcon(frame));
        });
    }

    @Override
    public void showMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            label.setIcon(null);
            label.setText(message);
        });
    }

    @Override
    public void onStopped() {
        SwingUtilities.invokeLater(() -> {
            label.setIcon(null);
            label.setText("Stopped");
        });
    }
}
