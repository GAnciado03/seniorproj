package videoapp;

import nu.pattern.OpenCV;
import videoapp.core.VideoPlayer;
import videoapp.ui.FullscreenManager;
import videoapp.ui.VideoPanelRenderer;
import videoapp.ui.VideoPlayerFrame;
import videoapp.util.CsvOverlayLoader;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Application entry point that initializes OpenCV, applies the system
 * look and feel, and displays the main video player window.
 *
 * @author Glenn Anciado
 * @version 2.0
 */

public final class Driver {
    private Driver() {}

    public static void main(String[] args) throws Exception {
        OpenCV.loadLocally();
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        SwingUtilities.invokeLater(() -> createFrame().setVisible(true));
    }

    private static VideoPlayerFrame createFrame() {
        VideoPanelRenderer panel = new VideoPanelRenderer();
        VideoPlayer player = new VideoPlayer(panel);
        FullscreenManager fullscreenManager = new FullscreenManager();
        CsvOverlayLoader overlayLoader = new CsvOverlayLoader();
        return new VideoPlayerFrame(panel, player, fullscreenManager, overlayLoader);
    }
}
