package videoapp.ui;

import videoapp.core.VideoRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Renders decoded frames and optional overlay geometry inside a Swing panel.
 * Keeps rendering logic encapsulated so other components can simply inject and reuse it.
 */

public class VideoPanelRenderer extends JPanel implements VideoRenderer {
    public enum ScalingMode {FIT, FILL, STRETCH, AUTO}

    private static final long MAX_INTERP_GAP_MS = 250;
    private static final long MAX_SHOW_AGE_MS = 300;

    private final AtomicBoolean packPending = new AtomicBoolean();
    private volatile BufferedImage frame;
    private volatile ScalingMode mode = ScalingMode.AUTO;
    private final List<OverlayPoint> overlayPoints = new CopyOnWriteArrayList<>();
    private final List<TimedOverlayPoint> timedOverlayPoints = new CopyOnWriteArrayList<>();
    private volatile long currentPosMs = 0L;
    private volatile long overlayTimeOffsetMs = 0L;

    public VideoPanelRenderer() {
        setBackground(new Color(18, 18, 18));
    }

    public void setMode(ScalingMode mode) {
        this.mode = mode;
        repaint();
    }

    public void setSurfaceBackground(Color color) {
        if (color != null) {
            setBackground(color);
            repaint();
        }
    }

    public ScalingMode getMode() {
        return mode;
    }

    @Override
    public void requestPackOnNextFrame() {
        packPending.set(true);
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
        BufferedImage currentFrame = frame;
        if (currentFrame == null) {
            return;
        }

        Graphics2D graphics = (Graphics2D) g.create();
        try {
            DrawArea drawArea = configureGraphics(graphics, currentFrame);
            graphics.drawImage(currentFrame, drawArea.x(), drawArea.y(), drawArea.width(), drawArea.height(), null);
            drawOverlays(graphics, drawArea);
            maybePackParent();
        } finally {
            graphics.dispose();
        }
    }

    private void maybePackParent() {
        if (!packPending.compareAndSet(true, false)) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            Window parent = SwingUtilities.getWindowAncestor(this);
            if (parent == null) {
                return;
            }
            GraphicsConfiguration gc = parent.getGraphicsConfiguration();
            GraphicsDevice device = (gc != null) ? gc.getDevice() : null;
            boolean fullscreen = device != null && device.getFullScreenWindow() == parent;
            if (!fullscreen) {
                parent.pack();
            }
        });
    }

    private DrawArea configureGraphics(Graphics2D graphics, BufferedImage img) {
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        GraphicsConfiguration gc = graphics.getDeviceConfiguration();
        AffineTransform deviceTx = (gc != null) ? gc.getDefaultTransform() : new AffineTransform();
        double scaleX = Math.max(deviceTx.getScaleX(), 1e-6);
        double scaleY = Math.max(deviceTx.getScaleY(), 1e-6);
        graphics.scale(1.0 / scaleX, 1.0 / scaleY);

        int deviceW = (int) Math.round(getWidth() * scaleX);
        int deviceH = (int) Math.round(getHeight() * scaleY);
        ScalingMode effectiveMode = resolveMode(deviceW, deviceH, img);
        Dimension drawSize = computeDrawSize(effectiveMode, img.getWidth(), img.getHeight(), deviceW, deviceH);
        int x = (deviceW - drawSize.width) / 2;
        int y = (deviceH - drawSize.height) / 2;
        return new DrawArea(x, y, drawSize.width, drawSize.height);
    }

    private ScalingMode resolveMode(int deviceW, int deviceH, BufferedImage img) {
        if (mode != ScalingMode.AUTO) {
            return mode;
        }
        double panelAR = deviceW / (double) deviceH;
        double videoAR = img.getWidth() / (double) img.getHeight();
        return (panelAR < videoAR) ? ScalingMode.FIT : ScalingMode.FILL;
    }

    private Dimension computeDrawSize(ScalingMode scaling,
                                      int imgW, int imgH,
                                      int deviceW, int deviceH) {
        int drawW;
        int drawH;
        if (scaling == ScalingMode.STRETCH) {
            drawW = deviceW;
            drawH = deviceH;
        } else if (scaling == ScalingMode.FILL) {
            double s = Math.max(deviceW / (double) imgW, deviceH / (double) imgH);
            drawW = Math.max(deviceW, (int) Math.ceil(imgW * s));
            drawH = Math.max(deviceH, (int) Math.ceil(imgH * s));
        } else {
            double s = Math.min(deviceW / (double) imgW, deviceH / (double) imgH);
            drawW = (int) Math.round(imgW * s);
            drawH = (int) Math.round(imgH * s);
        }
        return new Dimension(drawW, drawH);
    }

    private void drawOverlays(Graphics2D graphics, DrawArea drawArea) {
        if (timedOverlayPoints.isEmpty() && overlayPoints.isEmpty()) {
            return;
        }

        OverlayStyle style = OverlayStyle.from(drawArea);
        Object previousAA = graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Stroke previousStroke = graphics.getStroke();
        Font previousFont = graphics.getFont();

        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setStroke(new BasicStroke(style.strokePx()));
        graphics.setFont(previousFont.deriveFont((float) style.fontSize()));

        if (!timedOverlayPoints.isEmpty()) {
            OverlayLabel label = locateTimedOverlay(drawArea);
            if (label != null) {
                drawRingAndLabel(graphics, label, style);
            }
        } else {
            drawStaticOverlays(graphics, drawArea, style);
        }

        graphics.setStroke(previousStroke);
        graphics.setFont(previousFont);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, previousAA);
    }

    private void drawStaticOverlays(Graphics2D graphics, DrawArea drawArea, OverlayStyle style) {
        int label = 1;
        for (OverlayPoint point : overlayPoints) {
            OverlayLocation location = mapNorm(point.xNorm, point.yNorm, drawArea);
            drawRingAndLabel(graphics, new OverlayLabel(location.roundX(), location.roundY(), Integer.toString(label++)), style);
        }
    }

    private OverlayLabel locateTimedOverlay(DrawArea drawArea) {
        int n = timedOverlayPoints.size();
        if (n == 0) {
            return null;
        }

        int lo = 0;
        int hi = n - 1;
        int pos = n;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            long t = timedOverlayPoints.get(mid).timeMs;
            if (t >= currentPosMs) {
                pos = mid;
                hi = mid - 1;
            } else {
                lo = mid + 1;
            }
        }

        TimedOverlayPoint right = (pos < n) ? timedOverlayPoints.get(pos) : null;
        TimedOverlayPoint left = (pos - 1 >= 0) ? timedOverlayPoints.get(pos - 1) : null;

        OverlayLocation location = interpolateTimedPoint(left, right, drawArea);
        int nearestIndex = -1;
        if (location != null) {
            nearestIndex = pickNearestIndex(left, right, pos);
        } else {
            var fallback = pickNearestWithinWindow(left, right, pos, drawArea);
            if (fallback != null) {
                location = fallback.location();
                nearestIndex = fallback.index();
            }
        }

        if (location == null) {
            return null;
        }
        String label = (nearestIndex >= 0) ? Integer.toString(nearestIndex + 1) : "";
        return new OverlayLabel(location.roundX(), location.roundY(), label);
    }

    private OverlayLocation interpolateTimedPoint(TimedOverlayPoint left,
                                                  TimedOverlayPoint right,
                                                  DrawArea drawArea) {
        if (left == null || right == null || right.timeMs <= left.timeMs) {
            return null;
        }
        long gap = right.timeMs - left.timeMs;
        if (gap > MAX_INTERP_GAP_MS || currentPosMs < left.timeMs || currentPosMs > right.timeMs) {
            return null;
        }
        double weight = (currentPosMs - left.timeMs) / (double) gap;
        double xn = left.xNorm + (right.xNorm - left.xNorm) * weight;
        double yn = left.yNorm + (right.yNorm - left.yNorm) * weight;
        return mapNorm(xn, yn, drawArea);
    }

    private OverlayFallback pickNearestWithinWindow(TimedOverlayPoint left,
                                                    TimedOverlayPoint right,
                                                    int pos,
                                                    DrawArea drawArea) {
        TimedOverlayPoint nearest = null;
        long bestDt = Long.MAX_VALUE;
        int index = -1;
        if (left != null) {
            long dt = Math.abs(currentPosMs - left.timeMs);
            if (dt < bestDt) {
                bestDt = dt;
                nearest = left;
                index = pos - 1;
            }
        }
        if (right != null) {
            long dt = Math.abs(currentPosMs - right.timeMs);
            if (dt < bestDt) {
                bestDt = dt;
                nearest = right;
                index = pos;
            }
        }
        if (nearest == null || bestDt > MAX_SHOW_AGE_MS) {
            return null;
        }
        OverlayLocation location = mapNorm(nearest.xNorm, nearest.yNorm, drawArea);
        return new OverlayFallback(location, index);
    }

    private int pickNearestIndex(TimedOverlayPoint left, TimedOverlayPoint right, int pos) {
        if (left == null && right == null) {
            return -1;
        }
        if (left == null) {
            return pos;
        }
        if (right == null) {
            return pos - 1;
        }
        long leftDt = Math.abs(currentPosMs - left.timeMs);
        long rightDt = Math.abs(currentPosMs - right.timeMs);
        return (leftDt <= rightDt) ? (pos - 1) : pos;
    }

    public void setOverlayPoints(List<OverlayPoint> points) {
        overlayPoints.clear();
        if (points != null) {
            overlayPoints.addAll(points);
        }
        repaint();
    }

    public void setTimedOverlayPoints(List<TimedOverlayPoint> points) {
        timedOverlayPoints.clear();
        if (points != null) {
            List<TimedOverlayPoint> sorted = new java.util.ArrayList<>(points);
            sorted.sort(java.util.Comparator.comparingLong(p -> p.timeMs));
            timedOverlayPoints.addAll(sorted);
        }
        repaint();
    }

    public void setOverlayTimeOffsetMs(long offsetMs) {
        this.overlayTimeOffsetMs = offsetMs;
    }

    public long getOverlayTimeOffsetMs() {
        return overlayTimeOffsetMs;
    }

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

    public boolean hasTimedOverlayPoints() {
        return !timedOverlayPoints.isEmpty();
    }

    public Long getTimedOverlayRowTimeMs(int index1Based) {
        if (index1Based <= 0 || timedOverlayPoints.isEmpty()) {
            return null;
        }
        int idx = Math.min(index1Based - 1, timedOverlayPoints.size() - 1);
        if (idx < 0) {
            return null;
        }
        return timedOverlayPoints.get(idx).timeMs;
    }

    @Override
    public void onProgress(long posMs, long durationMs) {
        currentPosMs = posMs + overlayTimeOffsetMs;
        if (!timedOverlayPoints.isEmpty()) {
            repaint();
        }
    }

    private OverlayLocation mapNorm(double xn, double yn, DrawArea drawArea) {
        double px = drawArea.x() + (xn * drawArea.width());
        double py = drawArea.y() + ((1.0 - yn) * drawArea.height());
        return new OverlayLocation(px, py);
    }

    private void drawRingAndLabel(Graphics2D g, OverlayLabel label, OverlayStyle style) {
        int centerX = label.centerX();
        int centerY = label.centerY();
        int radius = style.radius();
        int cx = centerX - radius;
        int cy = centerY - radius;
        g.setColor(style.ringColor());
        g.drawOval(cx, cy, style.diameter(), style.diameter());
        g.setColor(style.textColor());
        FontMetrics fm = g.getFontMetrics();
        int textX = cx + style.diameter() + style.labelPad();
        int textY = cy + radius + (fm.getAscent() - fm.getDescent()) / 2;
        g.drawString(label.text(), textX, textY);
    }

    private record DrawArea(int x, int y, int width, int height) {}

    private record OverlayLocation(double x, double y) {
        int roundX() {
            return (int) Math.round(x);
        }

        int roundY() {
            return (int) Math.round(y);
        }
    }

    private record OverlayLabel(int centerX, int centerY, String text) {}

    private record OverlayStyle(int radius, int diameter, float strokePx,
                                int labelPad, Color ringColor, Color textColor,
                                int fontSize) {
        static OverlayStyle from(DrawArea area) {
            int radius = Math.max(12, Math.min(area.width(), area.height()) / 36);
            int diameter = radius * 2;
            float strokePx = Math.max(3f, Math.round(radius * 0.30f));
            int labelPad = Math.max(6, radius / 2);
            Color ringColor = new Color(255, 220, 0);
            Color textColor = new Color(80, 120, 220);
            int fontSize = Math.max(16, (int) Math.round(radius * 0.95));
            return new OverlayStyle(radius, diameter, strokePx, labelPad, ringColor, textColor, fontSize);
        }
    }

    private record OverlayFallback(OverlayLocation location, int index) {}
}
