package videoapp.ui;

import videoapp.util.AppPrefs;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.Objects;

/**
 * Applies and persists the UI theme so components stay consistent.
 *
 * @author Glenn Anciado
 * @version 1.0
 */
public final class ThemeController {
    private final JFrame frame;
    private final VideoPanelRenderer videoPanel;
    private final ProgressBar progressBar;
    private final JPanel controlsPanel;
    private final JPanel southPanel;
    private final JButton loadButton;
    private final AppPrefs prefs;

    private ThemePalette palette;
    private boolean darkModeEnabled;

    public ThemeController(JFrame frame,
                           VideoPanelRenderer videoPanel,
                           ProgressBar progressBar,
                           JPanel controlsPanel,
                           JPanel southPanel,
                           JButton loadButton) {
        this.frame = Objects.requireNonNull(frame, "frame");
        this.videoPanel = Objects.requireNonNull(videoPanel, "videoPanel");
        this.progressBar = Objects.requireNonNull(progressBar, "progressBar");
        this.controlsPanel = Objects.requireNonNull(controlsPanel, "controlsPanel");
        this.southPanel = Objects.requireNonNull(southPanel, "southPanel");
        this.loadButton = Objects.requireNonNull(loadButton, "loadButton");
        this.prefs = AppPrefs.get();
    }

    public void applyStoredPreference() {
        this.darkModeEnabled = prefs.isDarkMode();
        applyPalette();
    }

    public boolean isDarkModeEnabled() {
        return darkModeEnabled;
    }

    public void setDarkMode(boolean enabled) {
        if (this.darkModeEnabled == enabled) {
            return;
        }
        this.darkModeEnabled = enabled;
        prefs.setDarkMode(enabled);
        applyPalette();
    }

    public ThemePalette palette() {
        return palette;
    }

    private void applyPalette() {
        this.palette = ThemePalette.of(darkModeEnabled);
        Color frameBg = palette.windowBackground();
        frame.setBackground(frameBg);
        frame.getContentPane().setBackground(frameBg);
        JRootPane root = frame.getRootPane();
        root.putClientProperty("JRootPane.titleBarBackground", palette.windowBackground());
        root.putClientProperty("JRootPane.titleBarForeground", palette.textColor());

        controlsPanel.setBackground(palette.panelBackground());
        southPanel.setBackground(palette.panelBackground());
        progressBar.applyTheme(palette);
        videoPanel.setSurfaceBackground(palette.videoBackground());

        styleButton(loadButton, palette.controlBackground(), palette.controlForeground(), palette.borderColor());
    }

    private void styleButton(AbstractButton button, Color bg, Color fg, Color border) {
        button.setBackground(bg);
        button.setForeground(fg);
        button.setBorder(new LineBorder(border, 1, false));
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setUI(FlatButtonUI.get());
    }
}
