package videoapp.core;

import videoapp.util.FrameConverter;

import org.opencv.core.Mat;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import static java.lang.System.nanoTime;

/**
 * Worker thread that pulls frames from a VideoSource, converts them
 * to BufferedImage, renders via VideoRenderer,
 * handles pause/seek requests, and regulates timing based on FPS and speed.
 * Renders every grabbed frame (no frame skipping) for smooth playback.
 *
 * @author Glenn Anciado
 * @version 2.0
 */

public class PlaybackThread extends Thread{
    private final VideoSource source;
    private final VideoRenderer renderer;
    private final PlaybackConfig config;
    private final ProgressListener progressListener;
    private final ExecutorService frameConvertExecutor = Executors.newFixedThreadPool(
            2,
            r -> {
                Thread t = new Thread(r, "frame-converter");
                t.setDaemon(true);
                return t;
            });
    private static final int PIPELINE_DEPTH = 3;

    private final Object pauseLock = new Object();

    private volatile boolean playing = true;
    private volatile boolean stopRequested = false;
    private volatile boolean paused = false;

    private long pendingSeekMs = -1;

    public PlaybackThread(VideoSource source, VideoRenderer renderer, PlaybackConfig cfg, ProgressListener progressListener) {
        super("VideoPlaybackThread");
        this.source = source; this.renderer = renderer; this.config = cfg;
        this.progressListener = progressListener;
        setDaemon(true);
    }

    public void requestStop() {
        playing = false;
        stopRequested = true;
        interrupt();
        synchronized(pauseLock) {
            pauseLock.notifyAll();
        }
    }

    public void setPaused(boolean p) {
        synchronized (pauseLock) {
            paused = p;
            if(!paused) {
                pauseLock.notifyAll();
            }
        }
    }

    public void requestSeekMs(long ms) {
        synchronized (this) { pendingSeekMs = ms; }
        interrupt();
        synchronized(pauseLock){ pauseLock.notifyAll(); }
    }

    private long consumePendingSeekMs() {
        synchronized(this) {
            long v = pendingSeekMs;
            pendingSeekMs = -1;
            return v;
        }
    }

    private boolean hasPendingSeek() {
        synchronized (this) {
            return pendingSeekMs >= 0;
        }
    }

    @Override
    public void run() {
        boolean encounteredError = false;
        Mat frame = new Mat();
        Deque<FrameJob> pipeline = new ArrayDeque<>();
        try {
            double fps = source.fps();
            if(fps <= 0 || Double.isNaN(fps)) {
                fps = 30.0;
            }
            long duration = source.durationMs();
            long nextTickNs = System.nanoTime();
            while(playing) {
                synchronized (pauseLock) {
                    while(paused && playing) {
                        try {
                            pauseLock.wait();
                        } catch (Exception e) {
                        }
                    }
                }
                if(!playing) {
                    break;
                }

                boolean handledSeek = drainPendingSeek(duration, pipeline);
                if (handledSeek) {
                    nextTickNs = System.nanoTime();
                }

                while (pipeline.size() < PIPELINE_DEPTH && playing && !hasPendingSeek()) {
                    if(!source.grab()) {
                        playing = false;
                        break;
                    }
                    if (hasPendingSeek()) {
                        break;
                    }
                    if(!source.retrieve(frame) || frame.empty()) {
                        playing = false;
                        break;
                    }
                    if (hasPendingSeek()) {
                        break;
                    }
                    long pos = source.positionMs();
                    Mat matCopy = frame.clone();
                    Future<BufferedImage> future = frameConvertExecutor.submit(() -> FrameConverter.matToBufferedImage(matCopy));
                    pipeline.add(new FrameJob(matCopy, future, pos));
                }

                FrameJob job = pipeline.poll();
                if (job == null) {
                    if (!playing) {
                        break;
                    }
                    continue;
                }

                BufferedImage img = job.awaitImage();
                renderer.renderFrame(applyTargetResolution(img));

                long pos = job.positionMs();
                if(progressListener != null) progressListener.onProgress(pos, duration);
                renderer.onProgress(pos, duration);

                double sp = (config.speed <= 0) ? 1.0 : config.speed;
                long periodNs = (long) (1_000_000_000.0 / (fps * sp));
                nextTickNs += periodNs;
                long sleepNs = nextTickNs - nanoTime();
                if(sleepNs > 0) {
                    long ms = sleepNs / 1_000_000L;
                    int ns = (int) (sleepNs % 1_000_000L);
                    try {
                        Thread.sleep(ms, ns);
                    } catch (Exception e) {
                    }
                } else {
                    nextTickNs = nanoTime();
                }
            }
        } catch (Exception e) {
            encounteredError = true;
            throw e;
        } finally {
            frame.release();
            source.close();
            frameConvertExecutor.shutdownNow();
            clearJobs(pipeline);
            boolean completedNaturally = !stopRequested && !encounteredError;
            renderer.onPlaybackFinished(completedNaturally);
        }
    }

    private boolean drainPendingSeek(long duration, Deque<FrameJob> pipeline) {
        boolean handledSeek = false;
        long seek;
        while((seek = consumePendingSeekMs()) >= 0) {
            if(duration > 0) {
                seek = Math.max(0, Math.min(seek, duration));
            }
            source.seekMs(seek);
            handledSeek = true;
        }
        if (handledSeek) {
            clearJobs(pipeline);
        }
        return handledSeek;
    }

    private void clearJobs(Deque<FrameJob> pipeline) {
        if (pipeline == null) {
            return;
        }
        while (!pipeline.isEmpty()) {
            FrameJob job = pipeline.poll();
            if (job != null) {
                job.cancel();
            }
        }
    }

    private BufferedImage applyTargetResolution(BufferedImage img) {
        int targetW = config.targetWidth;
        int targetH = config.targetHeight;
        if (targetW <= 0 || targetH <= 0 ||
                (img.getWidth() == targetW && img.getHeight() == targetH)) {
            return img;
        }
        BufferedImage scaled = new BufferedImage(targetW, targetH,
                (img.getType() == BufferedImage.TYPE_CUSTOM) ? BufferedImage.TYPE_3BYTE_BGR : img.getType());
        Graphics2D g2 = scaled.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(img, 0, 0, targetW, targetH, null);
        } finally {
            g2.dispose();
        }
        return scaled;
    }

    private static final class FrameJob {
        private final Mat mat;
        private final Future<BufferedImage> future;
        private final long positionMs;

        FrameJob(Mat mat, Future<BufferedImage> future, long positionMs) {
            this.mat = mat;
            this.future = future;
            this.positionMs = positionMs;
        }

        BufferedImage awaitImage() {
            try {
                return future.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                mat.release();
            }
        }

        void cancel() {
            future.cancel(true);
            mat.release();
        }

        long positionMs() {
            return positionMs;
        }
    }
}
