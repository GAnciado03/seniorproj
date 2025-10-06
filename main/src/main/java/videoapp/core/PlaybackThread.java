package videoapp.core;

import videoapp.util.FrameConverter;

import org.opencv.core.Mat;

import java.awt.image.BufferedImage;
import static java.lang.System.nanoTime;

public class PlaybackThread extends Thread{
    private final VideoSource source;
    private final VideoRenderer renderer;
    private final PlaybackConfig config;
    private final ProgressListener progressListener;

    private final Object pauseLock = new Object();

    private volatile boolean playing = true;
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

    @Override
    public void run() {
        try {
            double fps = source.fps();
            if(fps <= 0 || Double.isNaN(fps)) {
                fps = 30.0;
            }
            long duration = source.durationMs();
            long nextTickNs = System.nanoTime();
            BufferedImage bufA = null, bufB = null;
            boolean useA = true;
            Mat frame = new Mat();

            while(playing) {
                synchronized (pauseLock) {
                    while(paused && playing) {
                        try {
                            pauseLock.wait();
                        } catch (Exception e) {
                        }
                    }
                }
                if(!playing) break;

                long seek = consumePendingSeekMs();
                if(seek >= 0 ) {
                    if(duration > 0) {
                        seek = Math.max(0, Math.min(seek, duration));
                    }
                        source.seekMs(seek);
                        nextTickNs = System.nanoTime();
                }
                if(!source.grab() || !source.retrieve(frame) || frame.empty()) break;

                if(useA) {
                    bufA = FrameConverter.matToBufferedImage(frame);
                    renderer.renderFrame(bufA);
                } else {
                    bufB = FrameConverter.matToBufferedImage(frame);
                }
                useA = !useA;

                long pos = source.positionMs();
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
        } finally {
            source.close();
            renderer.onStopped();
        }
    }
}
