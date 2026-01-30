package videoapp.ui;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages overlay points (static and timed) and paints them onto the video surface.
 *
 * @author Glenn Anciado
 * @version 1.0
 */
public final class OverlayRenderer {
    private static final long MAX_INTERP_GAP_MS = 250;
    private static final long MAX_SHOW_AGE_MS = 300;

    private final List<OverlayPoint> overlayPoints = new CopyOnWriteArrayList<>();
    private final List<TimedOverlayPoint> timedOverlayPoints = new CopyOnWriteArrayList<>();

    private volatile long overlayTimeOffsetMs = 0L;
    private volatile long currentPosMs = 0L;

    public void setOverlayPoints(List<OverlayPoint> points) {
        overlayPoints.clear();
        timedOverlayPoints.clear();
        if (points != null) {
            overlayPoints.addAll(points);
        }
    }

    public void setTimedOverlayPoints(List<TimedOverlayPoint> points) {
        timedOverlayPoints.clear();
        overlayPoints.clear();
        if (points != null) {
            List<TimedOverlayPoint> sorted = new ArrayList<>(points);
            sorted.sort(java.util.Comparator.comparingLong(p -> p.timeMs));
            timedOverlayPoints.addAll(sorted);
        }
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

    public void onProgress(long posMs) {
        currentPosMs = posMs + overlayTimeOffsetMs;
    }

    public void clear() {
        overlayPoints.clear();
        timedOverlayPoints.clear();
        overlayTimeOffsetMs = 0L;
        currentPosMs = 0L;
    }

    public List<? extends OverlayPoint> snapshotForHeatmap() {
        if (!timedOverlayPoints.isEmpty()) {
            return List.copyOf(timedOverlayPoints);
        }
        return List.copyOf(overlayPoints);
    }

    public void paint(Graphics2D graphics, VideoDrawArea drawArea) {
        if (drawArea == null || (timedOverlayPoints.isEmpty() && overlayPoints.isEmpty())) {
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

    private void drawStaticOverlays(Graphics2D graphics, VideoDrawArea drawArea, OverlayStyle style) {
        int label = 1;
        for (OverlayPoint point : overlayPoints) {
            OverlayLocation location = mapNorm(point.xNorm, point.yNorm, drawArea);
            drawRingAndLabel(graphics, new OverlayLabel(location.roundX(), location.roundY(), Integer.toString(label++)), style);
        }
    }

    private OverlayLabel locateTimedOverlay(VideoDrawArea drawArea) {
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
        int labelIndex = Math.max(0, pos - 1);
        if (location == null) {
            var fallback = pickNearestWithinWindow(left, right, pos, drawArea);
            if (fallback != null) {
                location = fallback.location();
                labelIndex = Math.max(0, fallback.index());
            }
        }

        if (location == null) {
            return null;
        }
        String label = Integer.toString(labelIndex + 1);
        return new OverlayLabel(location.roundX(), location.roundY(), label);
    }

    private OverlayLocation interpolateTimedPoint(TimedOverlayPoint left,
                                                  TimedOverlayPoint right,
                                                  VideoDrawArea drawArea) {
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
                                                    VideoDrawArea drawArea) {
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

    private OverlayLocation mapNorm(double xn, double yn, VideoDrawArea drawArea) {
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
        static OverlayStyle from(VideoDrawArea area) {
            int radius = Math.max(12, Math.min(area.width(), area.height()) / 36);
            int diameter = radius * 2;
            float strokePx = Math.max(1.5f, Math.round(radius * 0.18f));
            int labelPad = Math.max(6, radius / 2);
            Color ringColor = new Color(255, 220, 0);
            Color textColor = new Color(80, 120, 220);
            int fontSize = Math.max(16, (int) Math.round(radius * 0.95));
            return new OverlayStyle(radius, diameter, strokePx, labelPad, ringColor, textColor, fontSize);
        }
    }

    private record OverlayFallback(OverlayLocation location, int index) {}
}
