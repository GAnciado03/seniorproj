package videoapp.ui;

/**
 * Composite control with a play/pause button, a seek slider, and a time label.
 * Uses fixed-size vector icons so toggling doesn't shift layout.
 * Provides hooks for external play/pause action and seeking by fraction.
 *
 * @author Glenn Anciado
 * @version 1.0
 */

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.function.Consumer;

public class ProgressBar extends JPanel{
    private final JButton settings = new JButton();
    private final JButton fullscreen = new JButton();
    private final JButton play = new JButton();
    private final JSlider progress = new JSlider(0, 1000, 0);
    private final JLabel time = new JLabel("00:00 / 00:00");
    private boolean dragging = false;

    private Runnable onPlay;
    private Runnable onSettings;
    private Runnable onToggleFullscreen;
    private Consumer<Integer> progressFraction;

    private final Icon playIcon = new PlayPauseIcon(18, 18, PlayPauseIcon.Type.PLAY, Color.BLACK);
    private final Icon pauseIcon = new PlayPauseIcon(18, 18, PlayPauseIcon.Type.PAUSE, Color.BLACK);
    private final Icon settingsIcon = iconOrFallback("/icons/settings.png", 18, 18,
            new GearIcon(18, 18, new Color(80, 80, 80)));
    private final Icon fsEnterIcon = iconOrFallback("/icons/fullscreen.png", 18, 18,
            new FullscreenIcon(18, 18, FullscreenIcon.Mode.ENTER, new Color(80, 80, 80)));
    private final Icon fsExitIcon = iconOrFallback("/icons/minimize.png", 18, 18,
            new FullscreenIcon(18, 18, FullscreenIcon.Mode.EXIT, new Color(80, 80, 80)));

    public ProgressBar() {
        super(new BorderLayout(12, 0));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        play.setFocusable(false);
        play.setMargin(new Insets(4, 8, 4, 8));
        play.setText(null);
        play.setIcon(playIcon);
        play.setPreferredSize(new Dimension(36, 28));
        left.add(play);

        JPanel center = new JPanel(new BorderLayout(8, 0));
        center.add(progress, BorderLayout.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
        // Desired order: timestamp, settings, fullscreen
        right.add(time);

        settings.setFocusable(false);
        settings.setMargin(new Insets(4, 6, 4, 6));
        settings.setText(null);
        settings.setIcon(settingsIcon);
        settings.setPreferredSize(new Dimension(32, 28));
        right.add(settings);

        fullscreen.setFocusable(false);
        fullscreen.setMargin(new Insets(4, 6, 4, 6));
        fullscreen.setText(null);
        fullscreen.setIcon(fsEnterIcon);
        fullscreen.setPreferredSize(new Dimension(32, 28));
        right.add(fullscreen);
        add(left, BorderLayout.WEST);
        add(center, BorderLayout.CENTER);
        add(right, BorderLayout.EAST);

        progress.addChangeListener((ChangeEvent e) -> {
            if(progress.getValueIsAdjusting()) {
                dragging = true;
            } else if (dragging) {
                dragging = false;
                if(progressFraction != null) {
                    progressFraction.accept(progress.getValue());
                }
            }
        });

        play.addActionListener(e -> {
            if(onPlay != null) {
                onPlay.run();
            }
        });

        // Settings and Fullscreen actions
        settings.addActionListener(e -> {
            if (onSettings != null) onSettings.run();
        });
        fullscreen.addActionListener(e -> {
            if (onToggleFullscreen != null) onToggleFullscreen.run();
        });

        // Jump-to-click and drag mapping for better seeking/rewind
        progress.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                updateSliderFromMouse(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (progressFraction != null) {
                    progressFraction.accept(progress.getValue());
                }
                dragging = false;
            }
        });
        progress.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                updateSliderFromMouse(e);
            }
        });

    }

    private static Icon iconOrFallback(String resourcePath, int w, int h, Icon fallback) {
        try {
            URL url = ProgressBar.class.getResource(resourcePath);
            if (url == null) {
                int slash = resourcePath.lastIndexOf('/');
                String base = slash >= 0 ? resourcePath.substring(slash + 1) : resourcePath;
                url = ProgressBar.class.getResource("/" + base);
            }
            if (url != null) {
                ImageIcon ii = new ImageIcon(url);
                Image scaled = ii.getImage().getScaledInstance(Math.max(8, w), Math.max(8, h), Image.SCALE_SMOOTH);
                return new ImageIcon(scaled);
            }
        } catch (Throwable ignored) { }
        return fallback;
    }

    private void updateSliderFromMouse(MouseEvent e) {
        int w = progress.getWidth();
        if (w <= 0) return;
        int x = Math.max(0, Math.min(e.getX(), w));
        int v = (int) Math.round((x / (double) w) * progress.getMaximum());
        progress.setValue(v);
        dragging = true;
    }

    public void setOnPlay(Runnable r) {
        this.onPlay = r;
    }

    public void setProgressFraction(Consumer<Integer> c) {
        this.progressFraction = c;
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
        play.setIcon(p ? pauseIcon : playIcon);
    }

    public void setOnSettings(Runnable r) { this.onSettings = r; }
    public void setOnToggleFullscreen(Runnable r) { this.onToggleFullscreen = r; }
    public void setFullscreen(boolean fullscreenOn) {
        fullscreen.setIcon(fullscreenOn ? fsExitIcon : fsEnterIcon);
    }

    // Show a settings popup aligned to the right edge of the settings button
    public void showSettingsMenu(JPopupMenu menu) {
        if (menu == null) return;
        Dimension ps = menu.getPreferredSize();
        int x = settings.getWidth() - (ps != null ? ps.width : 0);
        int y = settings.getHeight();
        menu.show(settings, x, y);
    }
}
