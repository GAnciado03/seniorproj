package videoapp.ui;

/**
 * Swing-based VideoRenderer that draws frames onto a
 * JPanel with configurable scaling modes and DPI-aware
 * rendering.
 *
 * @author Glenn Anciado
 * @version 1.0
 */

import videoapp.core.VideoRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class VideoPanelRenderer extends JPanel implements VideoRenderer{
    public enum ScalingMode {FIT, FILL, STRETCH, AUTO};
    private volatile BufferedImage frame;
    private volatile boolean packNext = true;
    private volatile ScalingMode mode = ScalingMode.AUTO;
    private double scaleX = 1.0;
    private double scaleY = 1.0;
    private final List<OverlayPoint> overlayPoints = new CopyOnWriteArrayList<>();
    private final List<TimedOverlayPoint> timedOverlayPoints = new CopyOnWriteArrayList<>();
    private volatile long currentPosMs = 0L;
    private static final long TIME_WINDOW_MS = 50;

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

            // Draw overlay points as circles on top of the video
            if (!timedOverlayPoints.isEmpty() || !overlayPoints.isEmpty()) {
                final int radius = Math.max(4, Math.min(drawW, drawH) / 80); // adaptive size
                final int diameter = radius * 2;
                Composite old = graphics.getComposite();
                graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
                graphics.setColor(new Color(255, 64, 64));

                if (!timedOverlayPoints.isEmpty()) {
                    for (TimedOverlayPoint p : timedOverlayPoints) {
                        long dt = Math.abs(p.timeMs - currentPosMs);
                        if (dt > TIME_WINDOW_MS) continue;
                        double px = x + (p.xNorm * drawW);
                        double py = y + (p.yNorm * drawH);
                        int cx = (int)Math.round(px) - radius;
                        int cy = (int)Math.round(py) - radius;
                        graphics.fillOval(cx, cy, diameter, diameter);
                        graphics.setColor(Color.WHITE);
                        graphics.drawOval(cx, cy, diameter, diameter);
                        graphics.setColor(new Color(255, 64, 64));
                    }
                } else {
                    for (OverlayPoint p : overlayPoints) {
                        double px = x + (p.xNorm * drawW);
                        double py = y + (p.yNorm * drawH);
                        int cx = (int)Math.round(px) - radius;
                        int cy = (int)Math.round(py) - radius;
                        graphics.fillOval(cx, cy, diameter, diameter);
                        graphics.setColor(Color.WHITE);
                        graphics.drawOval(cx, cy, diameter, diameter);
                        graphics.setColor(new Color(255, 64, 64));
                    }
                }

                graphics.setComposite(old);
            }
        } finally {
            graphics.dispose();
        }
    }

    /**
     * Replace current static overlay points and repaint.
     */
    public void setOverlayPoints(List<OverlayPoint> points) {
        overlayPoints.clear();
        if (points != null) overlayPoints.addAll(points);
        repaint();
    }

    /**
     * Replace current time-synced overlay points and repaint.
     */
    public void setTimedOverlayPoints(List<TimedOverlayPoint> points) {
        timedOverlayPoints.clear();
        if (points != null) timedOverlayPoints.addAll(points);
        repaint();
    }

    @Override
    public void onProgress(long posMs, long durationMs) {
        currentPosMs = posMs;
        // repaint for time-synced overlays
        if (!timedOverlayPoints.isEmpty()) repaint();
    }
}
