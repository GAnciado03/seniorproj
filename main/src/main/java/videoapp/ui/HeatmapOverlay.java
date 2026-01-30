package videoapp.ui;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * Computes and renders a translucent heatmap showing the density of overlay
 * points on the video surface.
 *
 * @author Glenn Anciado
 * @version 2.0
 */

public final class HeatmapOverlay {
    private static final Color BASE_YELLOW = new Color(255, 213, 0);

    private final int rows;
    private final int cols;
    private final int[][] grid;
    private int maxCount;
    private boolean heatmapVisible;

    public HeatmapOverlay(int rows, int cols) {
        this.rows = Math.max(1, rows);
        this.cols = Math.max(1, cols);
        this.grid = new int[this.rows][this.cols];
    }

    public void rebuild(List<? extends OverlayPoint> points) {
        clearGrid();
        if (points == null || points.isEmpty()) {
            maxCount = 0;
            return;
        }
        for (OverlayPoint pt : points) {
            if (pt == null) continue;
            int col = clamp((int) Math.floor(pt.xNorm * cols), cols);
            int row = clamp((int) Math.floor((1.0 - pt.yNorm) * rows), rows);
            int count = ++grid[row][col];
            if (count > maxCount) {
                maxCount = count;
            }
        }
        heatmapVisible = false;
    }

    public void resetVisuals() {
        heatmapVisible = false;
    }

    public void applySnapshot(int[][] snapshot, int snapshotMax) {
        clearGrid();
        if (snapshot == null) {
            maxCount = 0;
            return;
        }
        for (int r = 0; r < Math.min(rows, snapshot.length); r++) {
            int[] rowData = snapshot[r];
            if (rowData == null) continue;
            for (int c = 0; c < Math.min(cols, rowData.length); c++) {
                grid[r][c] = rowData[c];
            }
        }
        maxCount = Math.max(0, snapshotMax);
    }

    public void onPlaybackStopped() {
        if (maxCount > 0) {
            heatmapVisible = true;
        }
    }

    public void paintHeatmap(Graphics2D g, Rectangle area) {
        if (!heatmapVisible || maxCount <= 0 || area == null) {
            return;
        }
        double cellW = area.getWidth() / cols;
        double cellH = area.getHeight() / rows;
        Composite previous = g.getComposite();
        g.setComposite(AlphaComposite.SrcOver);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int count = grid[r][c];
                if (count <= 0) continue;
                float intensity = Math.min(1f, count / (float) maxCount);
                Color tint = heatColor(intensity);
                double x = area.getX() + c * cellW;
                double y = area.getY() + r * cellH;
                g.setColor(tint);
                g.fill(new Rectangle2D.Double(x, y, cellW + 1, cellH + 1));
            }
        }
        g.setComposite(previous);
    }

    private void clearGrid() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c] = 0;
            }
        }
    }

    private int clamp(int value, int bound) {
        if (value < 0) return 0;
        if (value >= bound) return bound - 1;
        return value;
    }

    private Color heatColor(float intensity) {
        float clamped = Math.max(0f, Math.min(1f, intensity));
        int alpha = (int) Math.round(45 + (190 * clamped));
        alpha = Math.max(0, Math.min(255, alpha));
        return new Color(BASE_YELLOW.getRed(), BASE_YELLOW.getGreen(), BASE_YELLOW.getBlue(), alpha);
    }
}
