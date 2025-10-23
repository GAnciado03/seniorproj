package videoapp.ui;

/**
 * Main Swing window composing the video panel and basic controls.
 * Wires a VideoPlayer, file chooser, and resize
 * handling to adjust decode target size.
 *
 * @author Glenn Anciado
 * @version 1.2
 */

import videoapp.core.VideoPlayer;
import videoapp.ui.VideoPanelRenderer.ScalingMode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class VideoPlayerFrame extends JFrame{
    public VideoPlayerFrame() {
        super("Video Player");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        VideoPanelRenderer videoPanel = new VideoPanelRenderer();
        videoPanel.setMode(ScalingMode.AUTO);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        final JButton openBtn = new JButton("Open Video...");
        final JButton settingsBtn = new JButton("Settings");
        controls.add(openBtn);
        controls.add(settingsBtn);

        final ProgressBar progressBar = new ProgressBar();

        JPanel south = new JPanel(new BorderLayout());
        south.add(controls, BorderLayout.NORTH);
        south.add(progressBar, BorderLayout.SOUTH);

        add(videoPanel, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        final VideoPlayer player = new VideoPlayer(videoPanel);
        final AtomicInteger decodePercent = new AtomicInteger(100);
        final AtomicLong lastDurationMs = new AtomicLong(0);

        final Timer resizeDebounce = new Timer(140, e-> {
            int pw = Math.max(1, videoPanel.getWidth());
            int ph = Math.max(1, videoPanel.getHeight());

            double scale = Math.max(0.1, decodePercent.get() / 100.0);
            int tw = (int) Math.round(pw * scale);
            int th = (int) Math.round(ph * scale);

            if(tw < 160) tw = 160; else if (tw > 3840) tw = 3840;
            if((tw & 1) != 0) tw--;
            if(th < 90) th = 90; else if (th > 2160) th = 2160;
            if((th & 1) != 0) th--;

            player.setTargetSize(tw, th);
            videoPanel.requestPackOnNextFrame();

        });
        resizeDebounce.setRepeats(false);

        videoPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if(resizeDebounce.isRunning()) {
                    resizeDebounce.restart();
                } else {
                    resizeDebounce.start();
                }
            }
        });

        final VideoChooseHandler chooserHandler = new VideoChooseHandler(this, player);

        openBtn.addActionListener(e -> {
            chooserHandler.chooseToPlay();
            SwingUtilities.invokeLater(() -> progressBar.setPlayState(true));
        });

        settingsBtn.addActionListener(e -> {
            SettingsMenu menu = new SettingsMenu(
                speed -> SwingUtilities.invokeLater(() -> player.setSpeed(speed)),
                pct -> {
                    decodePercent.set(pct);
                    if(resizeDebounce.isRunning()) resizeDebounce.restart(); else resizeDebounce.start();
                },
                videoPanel.getWidth(),
                videoPanel.getHeight()
            );
            menu.show(settingsBtn, 0, settingsBtn.getHeight());
        });

        player.setProgressListener((pos, dur) -> SwingUtilities.invokeLater(() -> {
            lastDurationMs.set(dur);
            progressBar.setProgress(pos, dur);
            progressBar.setPlayState(!player.isPaused());
        }));

        progressBar.setOnPlay(() -> {
            player.togglePause();
            SwingUtilities.invokeLater(() -> progressBar.setPlayState(!player.isPaused()));
        });

        progressBar.setProgressFraction(pct -> {
            long dur = lastDurationMs.get();
            if (dur > 0) {
                long seek = Math.round(dur * (pct / 1000.0));
                player.seekMs(seek);
            }
        });

        setMinimumSize(new Dimension(600,400));
        pack();
        setLocationRelativeTo(null);

        resizeDebounce.start();
    }
}
