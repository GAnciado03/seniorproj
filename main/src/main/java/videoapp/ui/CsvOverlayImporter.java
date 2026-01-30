package videoapp.ui;

import videoapp.core.VideoPlayer;
import videoapp.util.CsvOverlayLoader;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Coordinates CSV selection and overlay loading for the video panel.
 *
 * @author Glenn Anciado
 * @version 1.0
 */

public final class CsvOverlayImporter {
    private final Component parent;
    private final VideoPanelRenderer videoPanel;
    private final VideoPlayer player;
    private final CsvOverlayLoader overlayLoader;
    private final Function<File, Long> overlayOffsetProvider;
    private final CsvChooserDelegate chooserDelegate;

    public CsvOverlayImporter(Component parent,
                              VideoPanelRenderer videoPanel,
                              VideoPlayer player,
                              CsvOverlayLoader overlayLoader,
                              Function<File, Long> overlayOffsetProvider,
                              CsvChooserDelegate chooserDelegate) {
        this.parent = Objects.requireNonNull(parent, "parent");
        this.videoPanel = Objects.requireNonNull(videoPanel, "videoPanel");
        this.player = Objects.requireNonNull(player, "player");
        this.overlayLoader = Objects.requireNonNull(overlayLoader, "overlayLoader");
        this.overlayOffsetProvider = Objects.requireNonNull(overlayOffsetProvider, "overlayOffsetProvider");
        this.chooserDelegate = Objects.requireNonNull(chooserDelegate, "chooserDelegate");
    }

    public File chooseCsvFile() {
        return promptForCsv();
    }

    public OverlayPayload loadOverlayPayload(File file, int heatRows, int heatCols) {
        List<TimedOverlayPoint> timedPoints = overlayLoader.loadTimed(file);
        if (!timedPoints.isEmpty()) {
            HeatmapSnapshot snapshot = buildSnapshot(timedPoints, heatRows, heatCols);
            String message = String.format("Loaded %d time-synced points from %s", timedPoints.size(), file.getName());
            return OverlayPayload.timedPayload(file, timedPoints, snapshot.grid(), snapshot.maxCount(), message);
        }
        List<OverlayPoint> points = overlayLoader.load(file);
        HeatmapSnapshot snapshot = buildSnapshot(points, heatRows, heatCols);
        String message = String.format("Loaded %d points from %s (no time column)", points.size(), file.getName());
        return OverlayPayload.staticPayload(file, points, snapshot.grid(), snapshot.maxCount(), message);
    }

    public void applyOverlayPayload(OverlayPayload payload) {
        if (payload == null) {
            return;
        }
        videoPanel.applyOverlayPayload(payload);
        if (payload.timed()) {
            double fps = player.fps();
            Long saved = overlayOffsetProvider.apply(payload.source());
            Long autoOffset = overlayLoader.suggestOffsetFromFrameIndex(payload.source(), fps);
            if (saved != null) {
                videoPanel.setOverlayTimeOffsetMs(saved);
            } else if (autoOffset != null) {
                videoPanel.setOverlayTimeOffsetMs(autoOffset);
            } else {
                videoPanel.setTimedOverlayAnchorIndex(1);
            }
        } else {
            videoPanel.setOverlayTimeOffsetMs(0);
        }
        JOptionPane.showMessageDialog(parent,
                payload.message(),
                "CSV Imported",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private File promptForCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select CSV with surfaceX,surfaceY");
        chooserDelegate.configure(chooser);
        File start = chooserDelegate.initialDirectory();
        if (start != null) {
            chooser.setCurrentDirectory(start);
        }
        if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            chooserDelegate.rememberSelection(file);
            return file;
        }
        return null;
    }

    private HeatmapSnapshot buildSnapshot(List<? extends OverlayPoint> points, int rows, int cols) {
        int[][] grid = new int[Math.max(1, rows)][Math.max(1, cols)];
        int max = 0;
        if (points != null) {
            for (OverlayPoint pt : points) {
                if (pt == null) continue;
                int col = clamp((int) Math.floor(pt.xNorm * cols), cols);
                int row = clamp((int) Math.floor((1.0 - pt.yNorm) * rows), rows);
                int count = ++grid[row][col];
                if (count > max) {
                    max = count;
                }
            }
        }
        return new HeatmapSnapshot(grid, max);
    }

    private int clamp(int value, int bound) {
        if (value < 0) return 0;
        if (value >= bound) return bound - 1;
        return value;
    }

    private record HeatmapSnapshot(int[][] grid, int maxCount) {}
}
