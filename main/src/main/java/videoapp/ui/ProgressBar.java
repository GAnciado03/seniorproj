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
import java.util.function.Consumer;

public class ProgressBar extends JPanel{
    private final JButton play = new JButton();
    private final JSlider progress = new JSlider(0, 1000, 0);
    private final JLabel time = new JLabel("00:00 / 00:00");
    private boolean dragging = false;

    private Runnable onPlay;
    private Consumer<Integer> progressFraction;

    private final Icon playIcon = new PlayPauseIcon(18, 18, PlayPauseIcon.Type.PLAY);
    private final Icon pauseIcon = new PlayPauseIcon(18, 18, PlayPauseIcon.Type.PAUSE);

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
        right.add(time);
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
}
