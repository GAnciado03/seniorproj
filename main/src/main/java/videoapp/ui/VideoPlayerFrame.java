package videoapp.ui;

/**
 * Main Swing window composing the video panel and basic controls.
 * Wires a VideoPlayer, file chooser, and resize
 * handling to adjust decode target size.
 *
 * @author Glenn Anciado
 * @version 1.6
 */

import videoapp.core.VideoPlayer;
import videoapp.ui.VideoPanelRenderer.ScalingMode;
import videoapp.util.CsvOverlayLoader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class VideoPlayerFrame extends JFrame{
    /**
     * Constructs and wires the main video player window. Builds the video panel
     * and playback controls, hooks up file open/CSV import actions, progress and
     * seek handling, fullscreen toggling, and resize-driven decode target updates.
     * Also initializes initial sizing and ensures the UI reflects play/pause state.
     *
     * @author Glenn Anciado
     * @version 1.6
     */
    public VideoPlayerFrame() {
        super("Video Player");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        VideoPanelRenderer videoPanel = new VideoPanelRenderer();
        videoPanel.setMode(ScalingMode.FIT);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        final JButton openBtn = new JButton("Open Video...");
        final JButton importCsvBtn = new JButton("Import CSV...");
        controls.add(openBtn);
        controls.add(importCsvBtn);

        final ProgressBar progressBar = new ProgressBar();

        JPanel south = new JPanel(new BorderLayout());
        south.add(controls, BorderLayout.NORTH);
        south.add(progressBar, BorderLayout.SOUTH);

        add(videoPanel, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        final VideoPlayer player = new VideoPlayer(videoPanel);
        final FullscreenManager fsMgr = new FullscreenManager();
        final FullscreenManager.State[] fsState = new FullscreenManager.State[1];
        final AtomicInteger decodePercent = new AtomicInteger(100);
        final java.util.concurrent.atomic.AtomicReference<Double> currentSpeed = new java.util.concurrent.atomic.AtomicReference<>(1.0);
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
            boolean wasFs = getGraphicsConfiguration().getDevice().getFullScreenWindow() == this;
            if (wasFs) {
                fsMgr.exit(this, fsState[0]);
                fsState[0] = null;
            }
            chooserHandler.chooseToPlay();
            player.pause();
            if (wasFs) {
                fsState[0] = fsMgr.enter(this);
            }
            SwingUtilities.invokeLater(() -> progressBar.setPlayState(false));
        });

        importCsvBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Select CSV with surfaceX,surfaceY");
            java.io.File videos = new java.io.File(
                new java.io.File(
                    new java.io.File(
                        new java.io.File(
                            new java.io.File(System.getProperty("user.home"), "OneDrive"),
                            "Documents"
                        ),
                        "GitHub"
                    ),
                    "seniorproj"
                ),
                "jgs-testing-data"
            );
            if (videos.isDirectory()) {
                chooser.setCurrentDirectory(videos);
            }
            int rv = chooser.showOpenDialog(this);
            if (rv == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                var timed = CsvOverlayLoader.loadTimed(file);
                if (!timed.isEmpty()) {
                    videoPanel.setTimedOverlayPoints(timed);
                    videoPanel.setTimedOverlayAnchorIndex(49);
                    try {
                        long dur = player.durationMs();
                        if (dur > 0 && timed.size() > 1600) {
                            long t0 = Math.max(0, timed.get(49 - 1).timeMs);
                            long t1 = Math.max(t0 + 1, timed.get(1600 - 1).timeMs);
                            long csvRange = Math.max(1, t1 - t0);
                            double scale = csvRange / (double) dur;
                            videoPanel.setOverlayTimeScale(scale);
                        }
                    } catch (Throwable ignore) {}
                    JOptionPane.showMessageDialog(this,
                            String.format("Loaded %d time-synced points from %s", timed.size(), file.getName()),
                            "CSV Imported",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    var points = CsvOverlayLoader.load(file);
                    videoPanel.setOverlayPoints(points);
                    JOptionPane.showMessageDialog(this,
                            String.format("Loaded %d points from %s (no time column)", points.size(), file.getName()),
                            "CSV Imported",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }
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

        progressBar.setOnSettings(() -> {
            SettingsMenu menu = new SettingsMenu(
                currentSpeed.get(),
                decodePercent.get(),
                speed -> {
                    currentSpeed.set(speed);
                    SwingUtilities.invokeLater(() -> player.setSpeed(speed));
                },
                pct -> {
                    decodePercent.set(pct);
                    if(resizeDebounce.isRunning()) resizeDebounce.restart(); else resizeDebounce.start();
                },
                videoPanel.getWidth(),
                videoPanel.getHeight()
            );
            progressBar.showSettingsMenu(menu);
        });

        progressBar.setFullscreen(false);
        progressBar.setOnToggleFullscreen(() -> {
            boolean isFs = getGraphicsConfiguration().getDevice().getFullScreenWindow() == this;
            if (isFs) {
                fsMgr.exit(this, fsState[0]);
                fsState[0] = null;
                progressBar.setFullscreen(false);
            } else {
                fsState[0] = fsMgr.enter(this);
                progressBar.setFullscreen(true);
            }
        });

        setMinimumSize(new Dimension(600,400));
        pack();
        setLocationRelativeTo(null);

        resizeDebounce.start();
    }
}
