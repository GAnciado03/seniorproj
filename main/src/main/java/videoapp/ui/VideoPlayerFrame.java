package videoapp.ui;

import videoapp.core.VideoPlayer;
import videoapp.ui.VideoPanelRenderer.ScalingMode;
import videoapp.util.CsvOverlayLoader;
import videoapp.util.OverlayOffsetStore;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.Insets;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Application entry point that initializes OpenCV, applies the system
 * look and feel, and displays the main video player window.
 *
 * @author Glenn Anciado
 * @version 2.0
 */

public class VideoPlayerFrame extends JFrame{
    private static final int MIN_W = 160;
    private static final int MAX_W = 3840;
    private static final int MIN_H = 90;
    private static final int MAX_H = 2160;

    private final VideoPanelRenderer videoPanel;
    private final VideoPlayer player;
    private final FullscreenManager fsMgr;
    private final CsvChooserDelegate csvChooserDelegate;
    private final Function<File, Long> overlayOffsetProvider;
    private final CsvOverlayImporter csvImporter;
    private final Timer resizeDebounce;
    private final ProgressBar progressBar;
    private final JPanel controlsPanel;
    private final JPanel southPanel;
    private final JButton loadButton;
    private final ThemeController themeController;
    private final Dimension defaultWindowSize;
    private final ExecutorService overlayLoaderExecutor;

    private int clampEven(int v, int min, int max) {
        int c = Math.max(min, Math.min(max, v));
        if ((c & 1) != 0) c--;
        return c;
    }

    private int[] computeScaledTarget(int pct) {
        int baseW = this.player.sourceWidth();
        int baseH = this.player.sourceHeight();
        if (baseW <= 0 || baseH <= 0) {
            baseW = Math.max(1, this.videoPanel.getWidth());
            baseH = Math.max(1, this.videoPanel.getHeight());
        }
        double scale = Math.max(0.1, pct / 100.0);
        int tw = clampEven((int) Math.round(baseW * scale), MIN_W, MAX_W);
        int th = clampEven((int) Math.round(baseH * scale), MIN_H, MAX_H);
        return new int[]{tw, th};
    }

    private void startOrRestart(Timer t) { if (t.isRunning()) t.restart(); else t.start(); }
    public VideoPlayerFrame() {
        this(new VideoPanelRenderer(), null, new FullscreenManager(), new CsvOverlayLoader());
    }

    public VideoPlayerFrame(VideoPanelRenderer panel, VideoPlayer player, FullscreenManager fsMgr) {
        this(panel, player, fsMgr, new CsvOverlayLoader());
    }

    public VideoPlayerFrame(VideoPanelRenderer panel,
                             VideoPlayer player,
                             FullscreenManager fsMgr,
                             CsvOverlayLoader overlayLoader) {
        this(panel, player, fsMgr, overlayLoader, new DefaultCsvChooserDelegate(), OverlayOffsetStore::get);
    }

    public VideoPlayerFrame(VideoPanelRenderer panel,
                            VideoPlayer player,
                            FullscreenManager fsMgr,
                            CsvOverlayLoader overlayLoader,
                            CsvChooserDelegate csvChooserDelegate,
                            Function<File, Long> overlayOffsetProvider) {
        super("Video Player");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        this.videoPanel = (panel != null) ? panel : new VideoPanelRenderer();
        this.videoPanel.setMode(ScalingMode.FIT);
        this.player = (player != null) ? player : new VideoPlayer(this.videoPanel);
        this.fsMgr = (fsMgr != null) ? fsMgr : new FullscreenManager();
        CsvOverlayLoader effectiveOverlayLoader = (overlayLoader != null) ? overlayLoader : new CsvOverlayLoader();
        this.csvChooserDelegate = (csvChooserDelegate != null) ? csvChooserDelegate : new DefaultCsvChooserDelegate();
        this.overlayOffsetProvider = (overlayOffsetProvider != null) ? overlayOffsetProvider : OverlayOffsetStore::get;

        this.loadButton = new JButton("Load CSV & Video...");
        this.loadButton.setFocusable(false);
        this.loadButton.setFocusPainted(false);
        this.loadButton.setMargin(new Insets(6, 14, 6, 14));
        this.loadButton.setPreferredSize(new Dimension(170, 30));
        this.loadButton.setOpaque(true);
        this.loadButton.setContentAreaFilled(true);
        this.loadButton.setUI(FlatButtonUI.get());
        this.controlsPanel = buildControlsPanel(this.loadButton);
        this.progressBar = new ProgressBar();
        this.southPanel = buildSouthPanel(this.controlsPanel, this.progressBar);
        this.csvImporter = new CsvOverlayImporter(
                this,
                this.videoPanel,
                this.player,
                effectiveOverlayLoader,
                this.overlayOffsetProvider,
                this.csvChooserDelegate
        );
        this.themeController = new ThemeController(
                this,
                this.videoPanel,
                this.progressBar,
                this.controlsPanel,
                this.southPanel,
                this.loadButton
        );

        add(this.videoPanel, BorderLayout.CENTER);
        add(this.southPanel, BorderLayout.SOUTH);

        this.overlayLoaderExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "overlay-loader");
            t.setDaemon(true);
            return t;
        });

        FullscreenManager.State[] fsState = new FullscreenManager.State[1];
        AtomicInteger decodePercent = new AtomicInteger(100);
        AtomicInteger lockedW = new AtomicInteger(0);
        AtomicInteger lockedH = new AtomicInteger(0);
        AtomicReference<Double> currentSpeed = new AtomicReference<>(1.0);
        AtomicLong lastDurationMs = new AtomicLong(0);

        this.resizeDebounce = createResizeDebounce(decodePercent, lockedW, lockedH);

        this.videoPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                startOrRestart(VideoPlayerFrame.this.resizeDebounce);
            }
        });

        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (fsState[0] != null && !isMaximized()) {
                    fsState[0] = null;
                    progressBar.setFullscreen(false);
                }
            }
        });

        VideoChooseHandler chooserHandler = new VideoChooseHandler(this, this.player);

        wireLoadButton(this.loadButton, fsState, chooserHandler);
        configureProgressUpdates(lastDurationMs);
        configureProgressInteractions(lastDurationMs, decodePercent, lockedW, lockedH, currentSpeed, resizeDebounce, fsState);

        setMinimumSize(new Dimension(600, 400));
        pack();
        this.defaultWindowSize = new Dimension(getSize());
        setLocationRelativeTo(null);

        this.themeController.applyStoredPreference();

        this.resizeDebounce.start();
    }

    @Override
    public void dispose() {
        this.overlayLoaderExecutor.shutdownNow();
        super.dispose();
    }

    private JPanel buildControlsPanel(JButton loadBtn) {
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(loadBtn);
        controls.setOpaque(true);
        return controls;
    }

    private JPanel buildSouthPanel(JPanel controls, ProgressBar progressBar) {
        JPanel south = new JPanel(new BorderLayout());
        south.add(controls, BorderLayout.NORTH);
        south.add(progressBar, BorderLayout.SOUTH);
        south.setOpaque(true);
        return south;
    }

    private Timer createResizeDebounce(AtomicInteger decodePercent,
                                       AtomicInteger lockedW,
                                       AtomicInteger lockedH) {
        Timer resizeDebounce = new Timer(140, e -> {
            int lw = lockedW.get();
            int lh = lockedH.get();
            if (lw > 0 && lh > 0) {
                this.player.setTargetSize(lw, lh);
            } else {
                int[] target = computeScaledTarget(decodePercent.get());
                this.player.setTargetSize(target[0], target[1]);
            }
        });
        resizeDebounce.setRepeats(false);
        return resizeDebounce;
    }

    private void wireLoadButton(JButton loadBtn,
                                FullscreenManager.State[] fsState,
                                VideoChooseHandler chooserHandler) {
        loadBtn.addActionListener(e -> handleLoadCsvAndVideo(fsState, chooserHandler));
    }

    private void handleLoadCsvAndVideo(FullscreenManager.State[] fsState,
                                       VideoChooseHandler chooserHandler) {
        File csvFile = this.csvImporter.chooseCsvFile();
        if (csvFile == null) {
            return;
        }
        SwingUtilities.invokeLater(this.videoPanel::hideHeatmapOverlay);
        this.videoPanel.showLoadingIndicator();
        this.overlayLoaderExecutor.submit(() -> {
            try {
                OverlayPayload payload = this.csvImporter.loadOverlayPayload(
                        csvFile,
                        this.videoPanel.heatmapRows(),
                        this.videoPanel.heatmapCols()
                );
                SwingUtilities.invokeLater(() -> {
                    this.csvImporter.applyOverlayPayload(payload);
                    this.videoPanel.hideLoadingIndicator();
                    this.videoPanel.hideHeatmapOverlay();
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    this.videoPanel.hideLoadingIndicator();
                    JOptionPane.showMessageDialog(this,
                            "Failed to import CSV: " + ex.getMessage(),
                            "CSV Import Error",
                            JOptionPane.ERROR_MESSAGE);
                });
            }
        });

        boolean wasFullscreen = fsState[0] != null;
        if (wasFullscreen) {
            this.fsMgr.exit(this, fsState[0]);
            fsState[0] = null;
            restoreDefaultSize();
        }

        File startDir = csvFile.getParentFile();
        chooserHandler.chooseToPlay(startDir);
        startOrRestart(this.resizeDebounce);

        this.player.pause();
        if (wasFullscreen) {
            fsState[0] = this.fsMgr.enter(this);
        }

        SwingUtilities.invokeLater(() -> this.progressBar.setPlayState(false));
    }

    private void configureProgressUpdates(AtomicLong lastDurationMs) {
        this.player.setProgressListener((pos, dur) -> SwingUtilities.invokeLater(() -> {
            lastDurationMs.set(dur);
            this.progressBar.setProgress(pos, dur);
            this.progressBar.setPlayState(!this.player.isPaused());
        }));
    }

    private void configureProgressInteractions(AtomicLong lastDurationMs,
                                               AtomicInteger decodePercent,
                                               AtomicInteger lockedW,
                                               AtomicInteger lockedH,
                                               AtomicReference<Double> currentSpeed,
                                               Timer resizeDebounce,
                                               FullscreenManager.State[] fsState) {
        this.progressBar.setOnPlay(() -> {
            this.player.togglePause();
            boolean playingNow = !this.player.isPaused();
            SwingUtilities.invokeLater(() -> {
                this.progressBar.setPlayState(playingNow);
                if (playingNow) {
                    this.videoPanel.hideHeatmapOverlay();
                }
            });
        });

        AtomicReference<Long> pendingSeekMs = new AtomicReference<>(-1L);
        this.progressBar.setProgressFraction(pct -> {
            long dur = lastDurationMs.get();
            if (dur <= 0) {
                return;
            }
            long seek = Math.round(dur * (pct / 1000.0));
            pendingSeekMs.set(Math.max(0, Math.min(seek, dur)));
        });

        this.progressBar.setOnSeekStart(this.videoPanel::requestSeekLoadingIndicator);
        this.progressBar.setOnSeekEnd(() -> SwingUtilities.invokeLater(() -> {
            this.videoPanel.requestSeekLoadingIndicator();
            long dur = lastDurationMs.get();
            long target = pendingSeekMs.getAndSet(-1L);
            if (dur > 0 && target >= 0) {
                this.player.seekMs(Math.max(0, Math.min(target, dur)));
            }
        }));

        this.progressBar.setOnSettings(() -> openSettingsMenu(currentSpeed, decodePercent, lockedW, lockedH));
        this.progressBar.setFullscreen(false);
        this.progressBar.setOnToggleFullscreen(() -> toggleFullscreen(fsState, this.progressBar));
    }

    private void openSettingsMenu(AtomicReference<Double> currentSpeed,
                                  AtomicInteger decodePercent,
                                  AtomicInteger lockedW,
                                  AtomicInteger lockedH) {
        int[] currentTarget = resolveTargetSize(lockedW, lockedH, decodePercent.get());
        SettingsMenu menu = new SettingsMenu(
                currentSpeed.get(),
                decodePercent.get(),
                speed -> {
                    currentSpeed.set(speed);
                    SwingUtilities.invokeLater(() -> this.player.setSpeed(speed));
                },
                pct -> {
                    decodePercent.set(pct);
                    lockedW.set(0);
                    lockedH.set(0);
                    int[] scaled = computeScaledTarget(decodePercent.get());
                    this.player.setTargetSize(scaled[0], scaled[1]);
                },
                (wSel, hSel) -> {
                    if (wSel != null && hSel != null && wSel > 0 && hSel > 0) {
                        lockedW.set(wSel);
                        lockedH.set(hSel);
                        this.player.setTargetSize(wSel, hSel);
                    }
                },
                effectiveSourceWidth(),
                effectiveSourceHeight(),
                currentTarget[0],
                currentTarget[1],
                this.themeController.isDarkModeEnabled(),
                this.themeController::setDarkMode
        );
        this.progressBar.showSettingsMenu(menu);
    }

    private int[] resolveTargetSize(AtomicInteger lockedW, AtomicInteger lockedH, int decodePercent) {
        int lw = lockedW.get();
        int lh = lockedH.get();
        if (lw > 0 && lh > 0) {
            return new int[]{lw, lh};
        }
        return computeScaledTarget(decodePercent);
    }

    private int effectiveSourceWidth() {
        int sw = this.player.sourceWidth();
        return (sw > 0) ? sw : Math.max(1, this.videoPanel.getWidth());
    }

    private int effectiveSourceHeight() {
        int sh = this.player.sourceHeight();
        return (sh > 0) ? sh : Math.max(1, this.videoPanel.getHeight());
    }

    private void toggleFullscreen(FullscreenManager.State[] fsState, ProgressBar progressBar) {
        boolean expanded = fsState[0] != null;
        if (expanded) {
            this.fsMgr.exit(this, fsState[0]);
            fsState[0] = null;
            progressBar.setFullscreen(false);
            restoreDefaultSize();
            return;
        }
        fsState[0] = this.fsMgr.enter(this);
        progressBar.setFullscreen(true);
    }

    private void restoreDefaultSize() {
        if (this.defaultWindowSize == null) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            setExtendedState(JFrame.NORMAL);
            setSize(this.defaultWindowSize);
            revalidate();
        });
    }

    private boolean isMaximized() {
        return (getExtendedState() & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH;
    }

}
