package videoapp.ui;

import videoapp.core.VideoRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Swing panel responsible for drawing video frames plus overlays, the heatmap,
 * and the loading indicator.
 *
 * @author Glenn Anciado
 * @version 2.0
 */
public class VideoPanelRenderer extends JPanel implements VideoRenderer {
    public enum ScalingMode {FIT, FILL, STRETCH, AUTO}

    private static final int HEAT_ROWS = 18;
    private static final int HEAT_COLS = 32;

    private final AtomicBoolean packPending = new AtomicBoolean();
    private volatile BufferedImage frame;
    private volatile ScalingMode mode = ScalingMode.AUTO;
    private final OverlayRenderer overlayRenderer = new OverlayRenderer();
    private final HeatmapOverlay heatmap = new HeatmapOverlay(HEAT_ROWS, HEAT_COLS);
    private final LoadingOverlay loadingOverlay = new LoadingOverlay(this::repaint);
    private final Timer seekSpinnerDelay;

    public VideoPanelRenderer() {
        setBackground(new Color(18, 18, 18));
        setPreferredSize(new Dimension(960, 540));
        this.seekSpinnerDelay = new Timer(140, e -> loadingOverlay.show());
        this.seekSpinnerDelay.setRepeats(false);
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

    public int heatmapRows() { return HEAT_ROWS; }
    public int heatmapCols() { return HEAT_COLS; }

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
        this.loadingOverlay.hide();
        repaint();
    }

    @Override
    public void showMessage(String message) {}

    @Override
    public void onStopped() {}

    @Override
    public void onPlaybackFinished(boolean completedNaturally) {
        if (completedNaturally) {
            heatmap.onPlaybackStopped();
        } else {
            heatmap.resetVisuals();
        }
        repaint();
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
            VideoDrawArea drawArea = configureGraphics(graphics, currentFrame);
            graphics.drawImage(currentFrame, drawArea.x(), drawArea.y(), drawArea.width(), drawArea.height(), null);
            Rectangle heatArea = new Rectangle(drawArea.x(), drawArea.y(), drawArea.width(), drawArea.height());
            heatmap.paintHeatmap(graphics, heatArea);
            drawOverlays(graphics, drawArea);
            loadingOverlay.paint(graphics, heatArea);
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

    private VideoDrawArea configureGraphics(Graphics2D graphics, BufferedImage img) {
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
        return new VideoDrawArea(x, y, drawSize.width, drawSize.height);
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

    private void drawOverlays(Graphics2D graphics, VideoDrawArea drawArea) {
        overlayRenderer.paint(graphics, drawArea);
    }

    public void setOverlayPoints(List<OverlayPoint> points) {
        overlayRenderer.setOverlayPoints(points);
        rebuildDensity();
        repaint();
    }

    public void setTimedOverlayPoints(List<TimedOverlayPoint> points) {
        overlayRenderer.setTimedOverlayPoints(points);
        rebuildDensity();
        repaint();
    }

    public void setOverlayTimeOffsetMs(long offsetMs) {
        overlayRenderer.setOverlayTimeOffsetMs(offsetMs);
    }

    public long getOverlayTimeOffsetMs() {
        return overlayRenderer.getOverlayTimeOffsetMs();
    }

    public void setTimedOverlayAnchorIndex(int index1Based) {
        overlayRenderer.setTimedOverlayAnchorIndex(index1Based);
    }

    public boolean hasTimedOverlayPoints() {
        return overlayRenderer.hasTimedOverlayPoints();
    }

    public Long getTimedOverlayRowTimeMs(int index1Based) {
        return overlayRenderer.getTimedOverlayRowTimeMs(index1Based);
    }

    @Override
    public void onProgress(long posMs, long durationMs) {
        overlayRenderer.onProgress(posMs);
        if (overlayRenderer.hasTimedOverlayPoints()) {
            repaint();
        }
    }

    public void resetDensityVisuals() {
        heatmap.resetVisuals();
        repaint();
    }

    public void showLoadingIndicator() {
        seekSpinnerDelay.stop();
        loadingOverlay.show();
    }

    public void requestSeekLoadingIndicator() {
        if (loadingOverlay.isActive()) {
            return;
        }
        if (seekSpinnerDelay.isRunning()) {
            seekSpinnerDelay.restart();
        } else {
            seekSpinnerDelay.start();
        }
    }

    public void hideLoadingIndicator() {
        seekSpinnerDelay.stop();
        loadingOverlay.hide();
    }

    public void applyOverlayPayload(OverlayPayload payload) {
        if (payload == null) {
            return;
        }
        if (payload.timed()) {
            overlayRenderer.setTimedOverlayPoints(payload.timedPoints());
        } else {
            overlayRenderer.setOverlayPoints(payload.staticPoints());
        }
        heatmap.applySnapshot(payload.heatmapGrid(), payload.heatmapMaxCount());
        repaint();
    }
    
    public void hideHeatmapOverlay() {
        heatmap.resetVisuals();
        repaint();
    }

    private void rebuildDensity() {
        List<? extends OverlayPoint> source = overlayRenderer.snapshotForHeatmap();
        heatmap.rebuild(source);
    }
}
