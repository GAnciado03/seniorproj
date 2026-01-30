package videoapp.ui;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.net.URL;
import java.util.function.Consumer;

/**
 * Composite control with a play/pause button, a seek slider, and a time label.
 * Uses fixed-size vector icons so toggling doesn't shift layout.
 * Provides hooks for external play/pause action and seeking by fraction.
 *
 * @author Glenn Anciado
 * @version 2.0
 */

public class ProgressBar extends JPanel{
    private final JButton settings = new JButton();
    private final JButton fullscreen = new JButton();
    private final JButton play = new JButton();
    private final JSlider progress = new JSlider(0, 1000, 0);
    private final JLabel time = new JLabel("00:00 / 00:00");
    private final JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
    private final JPanel centerPanel = new JPanel(new BorderLayout(8, 0));
    private final JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
    private boolean dragging = false;
    private boolean scrubbing = false;
    private boolean playing = false;
    private boolean fullscreenOn = false;
    private ThemePalette theme = ThemePalette.LIGHT;

    private Runnable onPlay;
    private Runnable onSettings;
    private Runnable onToggleFullscreen;
    private Consumer<Integer> progressFraction;
    private Runnable onSeekStart;
    private Runnable onSeekEnd;

    private Icon playIcon = new PlayPauseIcon(18, 18, PlayPauseIcon.Type.PLAY, Color.BLACK);
    private Icon pauseIcon = new PlayPauseIcon(18, 18, PlayPauseIcon.Type.PAUSE, Color.BLACK);
    private Icon settingsIcon = new GearIcon(18, 18, new Color(80, 80, 80));
    private Icon fsEnterIcon = new FullscreenIcon(18, 18, FullscreenIcon.Mode.ENTER, new Color(80, 80, 80));
    private Icon fsExitIcon = new FullscreenIcon(18, 18, FullscreenIcon.Mode.EXIT, new Color(80, 80, 80));

    public ProgressBar() {
        super(new BorderLayout(12, 0));

        play.setFocusable(false);
        play.setMargin(new Insets(4, 8, 4, 8));
        play.setText(null);
        play.setIcon(playIcon);
        play.setPreferredSize(new Dimension(36, 28));
        leftPanel.add(play);

        centerPanel.add(progress, BorderLayout.CENTER);

        rightPanel.add(time);

        settings.setFocusable(false);
        settings.setMargin(new Insets(4, 6, 4, 6));
        settings.setText(null);
        settings.setIcon(settingsIcon);
        settings.setPreferredSize(new Dimension(32, 28));
        rightPanel.add(settings);

        fullscreen.setFocusable(false);
        fullscreen.setMargin(new Insets(4, 6, 4, 6));
        fullscreen.setText(null);
        fullscreen.setIcon(fsEnterIcon);
        fullscreen.setPreferredSize(new Dimension(32, 28));
        rightPanel.add(fullscreen);
        add(leftPanel, BorderLayout.WEST);
        add(centerPanel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);

        progress.addChangeListener((ChangeEvent e) -> {
            if(progress.getValueIsAdjusting()) {
                beginScrub();
            } else {
                endScrub();
            }
        });

        play.addActionListener(e -> {
            if(onPlay != null) {
                onPlay.run();
            }
        });

        settings.addActionListener(e -> {
            if (onSettings != null) onSettings.run();
        });
        fullscreen.addActionListener(e -> {
            if (onToggleFullscreen != null) onToggleFullscreen.run();
        });

        progress.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                beginScrub();
                updateSliderFromMouse(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                endScrub();
            }
        });
        progress.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                updateSliderFromMouse(e);
            }
        });
        applyTheme(theme);
    }

    public void applyTheme(ThemePalette palette) {
        if (palette == null) {
            return;
        }
        this.theme = palette;
        Color chrome = palette.panelBackground();
        Color surface = palette.surfaceBackground();
        Color controlBg = palette.windowBackground();
        Color controlFg = palette.controlForeground();

        setBackground(chrome);
        leftPanel.setBackground(chrome);
        rightPanel.setBackground(chrome);
        centerPanel.setBackground(chrome);
        setOpaque(true);
        leftPanel.setOpaque(true);
        rightPanel.setOpaque(true);
        centerPanel.setOpaque(true);

        progress.setBackground(surface);
        progress.setForeground(palette.accentColor());
        progress.setBorder(new LineBorder(palette.borderColor()));
        progress.setOpaque(true);

        time.setForeground(palette.textColor());

        JButton[] buttons = {play, settings, fullscreen};
        for (JButton button : buttons) {
            button.setBackground(controlBg);
            button.setForeground(controlFg);
            button.setBorder(new LineBorder(palette.borderColor(), 1, false));
            button.setOpaque(false);
            button.setContentAreaFilled(false);
            button.setFocusPainted(false);
            button.setUI(FlatButtonUI.get());
        }

        rebuildIcons(palette);
        setPlayState(this.playing);
        setFullscreen(this.fullscreenOn);
    }

    private void rebuildIcons(ThemePalette palette) {
        Color iconColor = (palette.iconColor() != null) ? palette.iconColor() : Color.BLACK;
        boolean dark = palette.isDark();
        this.playIcon = new PlayPauseIcon(18, 18, PlayPauseIcon.Type.PLAY, iconColor);
        this.pauseIcon = new PlayPauseIcon(18, 18, PlayPauseIcon.Type.PAUSE, iconColor);
        this.settingsIcon = loadIcon(
                dark ? "/settings_darkmode.png" : "/settings.png",
                18, 18,
                new GearIcon(18, 18, iconColor));
        this.fsEnterIcon = loadIcon(
                dark ? "/fullscreen_darkmode.png" : "/fullscreen.png",
                18, 18,
                new FullscreenIcon(18, 18, FullscreenIcon.Mode.ENTER, iconColor));
        this.fsExitIcon = loadIcon(
                dark ? "/minimize_darkmode.png" : "/minimize.png",
                18, 18,
                new FullscreenIcon(18, 18, FullscreenIcon.Mode.EXIT, iconColor));
        settings.setIcon(settingsIcon);
        fullscreen.setIcon(fullscreenOn ? fsExitIcon : fsEnterIcon);
    }

    private Icon loadIcon(String resourcePath, int width, int height, Icon fallback) {
        try {
            URL url = ProgressBar.class.getResource(resourcePath);
            if (url != null) {
                ImageIcon icon = new ImageIcon(url);
                Image scaled = icon.getImage().getScaledInstance(Math.max(8, width), Math.max(8, height), Image.SCALE_SMOOTH);
                return new ImageIcon(scaled);
            }
        } catch (Exception ignore) { }
        return fallback;
    }

    private void updateSliderFromMouse(MouseEvent e) {
        int w = progress.getWidth();
        if (w <= 0) return;
        int x = Math.max(0, Math.min(e.getX(), w));
        int v = (int) Math.round((x / (double) w) * progress.getMaximum());
        progress.setValue(v);
        dragging = true;
        if (progressFraction != null) {
            progressFraction.accept(progress.getValue());
        }
    }

    private void beginScrub() {
        if (!scrubbing) {
            scrubbing = true;
            dragging = true;
            if (onSeekStart != null) {
                onSeekStart.run();
            }
        }
    }

    private void endScrub() {
        if (!scrubbing) {
            return;
        }
        scrubbing = false;
        dragging = false;
        if (progressFraction != null) {
            progressFraction.accept(progress.getValue());
        }
        if (onSeekEnd != null) {
            onSeekEnd.run();
        }
    }

    public void setOnPlay(Runnable r) {
        this.onPlay = r;
    }

    public void setProgressFraction(Consumer<Integer> c) {
        this.progressFraction = c;
    }

    public void setOnSeekStart(Runnable r) {
        this.onSeekStart = r;
    }

    public void setOnSeekEnd(Runnable r) {
        this.onSeekEnd = r;
    }

    public void setProgress(long posMs, long durMs) {
        time.setText(fmt(posMs) + " / " + fmt(durMs));
        if(!dragging && durMs > 0 ){
            int v = (int) Math.round((posMs / (double) durMs) * 1000.0);
            progress.setValue(Math.max(0, Math.min(1000, v)));
        }
    }

    public static String fmt(long ms) {
        long s = Math.max(0, ms) / 1000;
        long h = s / 1000, m = (s % 3600) / 60, sec = s % 60;
        return (h > 0) ? String.format("%d:%02d:%02d", h, m, sec) :
            String.format("%02d:%02d", m, sec);
    }

    public void reset() {
        setPlayState(false);
        progress.setValue(0);
        time.setText("00:00 / 00:00");
    }

    public void setPlayState(boolean p) {
        this.playing = p;
        play.setIcon(p ? pauseIcon : playIcon);
    }

    public void setOnSettings(Runnable r) { this.onSettings = r; }
    public void setOnToggleFullscreen(Runnable r) { this.onToggleFullscreen = r; }
    public void setFullscreen(boolean fullscreenOn) {
        this.fullscreenOn = fullscreenOn;
        fullscreen.setIcon(fullscreenOn ? fsExitIcon : fsEnterIcon);
    }

    public void showSettingsMenu(JPopupMenu menu) {
        if (menu == null) return;
        Dimension ps = menu.getPreferredSize();
        int x = settings.getWidth() - (ps != null ? ps.width : 0);
        int y = settings.getHeight();
        menu.show(settings, x, y);
    }
}
