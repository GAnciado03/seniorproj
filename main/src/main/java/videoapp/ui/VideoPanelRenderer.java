package videoapp.ui;

/**
 * Swing-based VideoRenderer that draws frames onto a
 * JPanel with configurable scaling modes and DPI-aware
 * rendering.
 *
 * @author Glenn Anciado
 * @version 2.0
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
    private volatile long overlayTimeOffsetMs = 0L;
    
    private volatile double overlayTimeScale = 1.0;
    
    private volatile boolean flipY = false;
    private volatile boolean swapXY = false;
    private static final long MAX_INTERP_GAP_MS = 250;
    private static final long MAX_SHOW_AGE_MS  = 300;

    public VideoPanelRenderer() {
        setBackground(new Color(18, 18, 18));
    }

    public void setMode(ScalingMode mode) {
        /**
         * Sets how the video is scaled within the panel (fit, fill, stretch, or auto).
         * Triggers a repaint so the new mode is reflected immediately.
         *
         * @author Glenn Anciado
         * @version 2.0
         */
        this.mode = mode;
        repaint();
    }

    public ScalingMode getMode() {
        /**
         * Returns the current scaling mode used by the renderer.
         *
         * @author Glenn Anciado
         * @version 2.0
         */
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
            AffineTransform deviceTx = (gc != null) ? gc.getDefaultTransform() : new AffineTransform();
            scaleX = deviceTx.getScaleX();
            scaleY = deviceTx.getScaleY();
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

            
            if (!timedOverlayPoints.isEmpty() || !overlayPoints.isEmpty()) {
                final int radius = Math.max(6, Math.min(drawW, drawH) / 80);
                final int diameter = radius * 2;
                final float strokePx = Math.max(2f, Math.min(10f, Math.min(drawW, drawH) / 180f));
                final Color ringColor = new Color(255, 220, 0);
                final Color textColor = new Color(80, 120, 220);

                Object aaOld = graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Stroke oldStroke = graphics.getStroke();
                graphics.setStroke(new BasicStroke(strokePx));

                Font oldFont = graphics.getFont();
                int fontSize = Math.max(12, (int)Math.round(diameter * 0.9));
                graphics.setFont(oldFont.deriveFont((float) fontSize));

                if (!timedOverlayPoints.isEmpty()) {
                    TimedOverlayPoint left = null, right = null;
                    int n = timedOverlayPoints.size();
                    int lo = 0, hi = n - 1, pos = n;
                    while (lo <= hi) {
                        int mid = (lo + hi) >>> 1;
                        long t = timedOverlayPoints.get(mid).timeMs;
                        if (t >= currentPosMs) { pos = mid; hi = mid - 1; }
                        else { lo = mid + 1; }
                    }
                    if (pos < n) right = timedOverlayPoints.get(pos);
                    if (pos - 1 >= 0) left = timedOverlayPoints.get(pos - 1);

                    Double drawX = null, drawY = null;
                    int nearestIndex = -1;
                    if (left != null && right != null && right.timeMs > left.timeMs) {
                        long gap = right.timeMs - left.timeMs;
                        if (gap <= MAX_INTERP_GAP_MS && currentPosMs >= left.timeMs && currentPosMs <= right.timeMs) {
                            double w = (currentPosMs - left.timeMs) / (double) gap;
                            double xn = left.xNorm + (right.xNorm - left.xNorm) * w;
                            double yn = left.yNorm + (right.yNorm - left.yNorm) * w;
                            double[] mapped = mapNorm(xn, yn, x, y, drawW, drawH);
                            drawX = mapped[0];
                            drawY = mapped[1];
                            nearestIndex = (Math.abs(currentPosMs - left.timeMs) <= Math.abs(currentPosMs - right.timeMs))
                                    ? (pos - 1) : pos;
                        }
                    }
                    if (drawX == null || drawY == null) {
                        TimedOverlayPoint nearest = null;
                        long bestDt = Long.MAX_VALUE;
                        if (left != null) {
                            long dt = Math.abs(currentPosMs - left.timeMs);
                            if (dt < bestDt) { bestDt = dt; nearest = left; nearestIndex = pos - 1; }
                        }
                        if (right != null) {
                            long dt = Math.abs(currentPosMs - right.timeMs);
                            if (dt < bestDt) { bestDt = dt; nearest = right; nearestIndex = pos; }
                        }
                        if (nearest != null && bestDt <= MAX_SHOW_AGE_MS) {
                            double[] mapped = mapNorm(nearest.xNorm, nearest.yNorm, x, y, drawW, drawH);
                            drawX = mapped[0];
                            drawY = mapped[1];
                        }
                    }

                    if (drawX != null && drawY != null) {
                        int cx = (int)Math.round(drawX) - radius;
                        int cy = (int)Math.round(drawY) - radius;
                        graphics.setColor(ringColor);
                        graphics.drawOval(cx, cy, diameter, diameter);

                        String label = (nearestIndex >= 0) ? Integer.toString(nearestIndex + 1) : "";
                        graphics.setColor(textColor);
                        FontMetrics fm = graphics.getFontMetrics();
                        int textW = fm.stringWidth(label);
                        int textX = cx + radius - (textW / 2);
                        int textY = cy + radius + (fm.getAscent() - fm.getDescent()) / 2;
                        graphics.drawString(label, textX, textY);
                    }
                } else {
                    int i = 1;
                    for (OverlayPoint p : overlayPoints) {
                        double[] mapped = mapNorm(p.xNorm, p.yNorm, x, y, drawW, drawH);
                        double px = mapped[0];
                        double py = mapped[1];
                        int cx = (int)Math.round(px) - radius;
                        int cy = (int)Math.round(py) - radius;

                        graphics.setColor(ringColor);
                        graphics.drawOval(cx, cy, diameter, diameter);

                        String label = Integer.toString(i++);
                        graphics.setColor(textColor);
                        FontMetrics fm = graphics.getFontMetrics();
                        int textW = fm.stringWidth(label);
                        int textX = cx + radius - (textW / 2);
                        int textY = cy + radius + (fm.getAscent() - fm.getDescent()) / 2;
                        graphics.drawString(label, textX, textY);
                    }
                }

                graphics.setStroke(oldStroke);
                graphics.setFont(oldFont);
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aaOld);
            }
        } finally {
            graphics.dispose();
        }
    }

    /**
     * Replaces the current static overlay points and repaints the panel.
     * Points are interpreted as normalized positions relative to the drawn video area.
     *
     * @author Glenn Anciado
     * @version 2.0
     */
    public void setOverlayPoints(List<OverlayPoint> points) {
        overlayPoints.clear();
        if (points != null) overlayPoints.addAll(points);
        repaint();
    }

    /**
     * Replaces the current time-synced overlay points and repaints.
     * The list is sorted by timestamp to enable fast lookup and interpolation.
     *
     * @author Glenn Anciado
     * @version 2.0
     */
    public void setTimedOverlayPoints(List<TimedOverlayPoint> points) {
        timedOverlayPoints.clear();
        if (points != null) {
            java.util.ArrayList<TimedOverlayPoint> sorted = new java.util.ArrayList<>(points);
            sorted.sort(java.util.Comparator.comparingLong(p -> p.timeMs));
            timedOverlayPoints.addAll(sorted);
        }
        repaint();
    }

    /**
     * Applies a time offset so overlays are shifted relative to the video timeline.
     * Positive values move overlays later; negative values move them earlier.
     *
     * @author Glenn Anciado
     * @version 2.0
     */
    public void setOverlayTimeOffsetMs(long offsetMs) {
        this.overlayTimeOffsetMs = offsetMs;
    }

    /**
     * Returns the current overlay time offset in milliseconds.
     *
     * @author Glenn Anciado
     * @version 2.0
     */
    public long getOverlayTimeOffsetMs() {
        return overlayTimeOffsetMs;
    }

    /**
     * Anchors the overlay so that the specified 1-based CSV row aligns with video t=0.
     * If the index is invalid, the anchor is cleared.
     *
     * @author Glenn Anciado
     * @version 2.0
     */
    public void setTimedOverlayAnchorIndex(int index1Based) {
        if (index1Based <= 0 || timedOverlayPoints.isEmpty()) {
            this.overlayTimeOffsetMs = 0L;
            return;
        }
        int idx = Math.min(index1Based - 1, timedOverlayPoints.size() - 1);
        if (idx >= 0) {
            this.overlayTimeOffsetMs = -timedOverlayPoints.get(idx).timeMs;
        }
    }

    /**
     * Receives playback progress from the player, updates internal time (with scaling
     * and offset), and requests repaint when time-synced overlays are active.
     *
     * @author Glenn Anciado
     * @version 2.0
     */
    @Override
    public void onProgress(long posMs, long durationMs) {
        long scaled = (long) Math.round(posMs * (overlayTimeScale <= 0 ? 1.0 : overlayTimeScale));
        currentPosMs = scaled + overlayTimeOffsetMs;
        if (!timedOverlayPoints.isEmpty()) repaint();
    }

    private double[] mapNorm(double xn, double yn, int x, int y, int drawW, int drawH) {
        double fx = xn, fy = yn;
        if (swapXY) {
            double tmp = fx; fx = fy; fy = tmp;
        }
        if (flipY) {
            fy = 1.0 - fy;
        }
        double px = x + (fx * drawW);
        double py = y + (fy * drawH);
        return new double[]{px, py};
    }

    /** Returns whether the Y axis is flipped when mapping overlay points. */
    public boolean isFlipY() { return flipY; }
    /** Returns whether X and Y are swapped when mapping overlay points. */
    public boolean isSwapXY() { return swapXY; }
    /** Enables or disables Y-axis flip for overlay mapping. */
    public void setFlipY(boolean v) { this.flipY = v; }
    /** Enables or disables X/Y swap for overlay mapping. */
    public void setSwapXY(boolean v) { this.swapXY = v; }
    /** Sets a time scale applied to video time before aligning overlays. */
    public void setOverlayTimeScale(double s) { this.overlayTimeScale = (s > 0 && Double.isFinite(s)) ? s : 1.0; }
    /** Returns the current overlay time scale factor. */
    public double getOverlayTimeScale() { return overlayTimeScale; }
}
