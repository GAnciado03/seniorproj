package videoapp.ui;

import videoapp.core.VideoRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

public class VideoPanelRenderer extends JPanel implements VideoRenderer{
    public enum ScalingMode {FIT, FILL, STRETCH, AUTO};
    private volatile BufferedImage frame;
    private volatile boolean packNext = true;
    private volatile ScalingMode mode = ScalingMode.AUTO;
    private double scaleX = 1.0;
    private double scaleY = 1.0;

    public VideoPanelRenderer() {
        setBackground(new Color(18, 18, 18));
    }

    public void setMode(ScalingMode mode) {
        this.mode = mode;
        repaint();
    }

    public ScalingMode getMode() {
        return mode;
    }

    @Override
    public void requestPackOnNextFrame() {
        packNext = true;
    }

    @Override
    public void renderFrame(BufferedImage img) {
        this.frame = img;
        repaint();
    }

    @Override
    public void showMessage(String message) {}

    @Override
    public void onStopped() {

    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if(frame == null) {
            return;
        }

        Graphics2D graphics = (Graphics2D) g.create();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            var gc = graphics.getDeviceConfiguration();
            var tx = (gc != null) ? gc.getDefaultTransform() : new AffineTransform();
            scaleX = tx.getScaleX();
            scaleY = tx.getScaleY();
            graphics.scale(1.0 / scaleX, 1.0 / scaleY);
            int imgW = frame.getWidth();
            int imgH = frame.getHeight();

            int deviceW = (int) Math.round(getWidth() * scaleX);
            int deviceH = (int) Math.round(getHeight() * scaleY);

            ScalingMode eff = mode;
            if(eff == ScalingMode.AUTO) {
                double panelAR = deviceW / (double) deviceH;
                double videoAR = imgW / (double) imgH;
                eff = (panelAR < videoAR) ? ScalingMode.FIT : ScalingMode.FILL;
            }

            int drawH;
            int drawW;
            if (eff == ScalingMode.STRETCH) {
                drawW = deviceW;
                drawH = deviceH;
            } else if (eff == ScalingMode.FILL) {
                double s = Math.max(deviceW / (double) imgW, deviceH / (double) imgH);
                drawW = (int) Math.ceil(imgW * s);
                drawH = (int) Math.ceil(imgH * s);
                if(drawW < deviceW) {
                    drawW = deviceW;
                }
                if(drawH < deviceH) {
                    drawH = deviceH;
                }
            } else {
                double s = Math.min(deviceW / (double) imgW, deviceH / (double) imgH);
                drawW = (int) Math.round(imgW * s);
                drawH = (int) Math.round(imgH * s);
            }
            int x = (deviceW - drawW) / 2;
            int y = (deviceH - drawH) / 2;

            graphics.drawImage(frame, x ,y, drawW, drawH, null);
        } finally {
            graphics.dispose();
        }
    }
}
