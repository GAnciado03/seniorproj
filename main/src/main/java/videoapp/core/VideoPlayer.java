package videoapp.core;

public class VideoPlayer {
    public PlaybackThread thread;
    private VideoSource capture;
    private boolean paused = false;
    private final VideoRenderer renderer;
    private final PlaybackConfig config = new PlaybackConfig();
    private ProgressListener progressListener;

    public VideoPlayer(VideoRenderer renderer) {
        this.renderer = renderer;
    }

    public synchronized boolean play(String path) {
        stop(); paused = false;
        capture = new VideoSource();
        if(!capture.open(path)) {
            renderer.showMessage("Failed to open: " + path);
            capture = null;
            return false;
        }
        thread = new PlaybackThread(capture, renderer, config, progressListener);
        thread.start();
        return true;
    }

    public synchronized void stop() {
        if (thread != null) {
            thread.requestStop();
            try {
                thread.join(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            thread = null;
        }
        if (capture != null) {
            capture.close();
            capture = null;
        }
        renderer.onStopped();
        paused = false;
    }

    public synchronized void pause() {
        if(thread != null) {
            thread.setPaused(true);
            paused = true;
        }
    }
    public synchronized void resume() {
        if(thread != null) {
            thread.setPaused(false);
            paused = false;
        }
    }
    public synchronized void togglePause() {
        if(paused) {
            resume();
        } else {
            pause();
        }
    }
    public synchronized boolean isPaused(){
        return paused;
    }

    public synchronized void seekMs(long ms) {
        if(thread != null) {
            thread.requestSeekMs(ms);
        }
    }
    public synchronized void setSpeed(double speed) {
        config.speed = speed < 0 ? 1.0 : speed;
    }
    public synchronized void setResolution(int width, int height) {
        config.targetWidth = Math.max(0, width);
        config.targetHeight = Math.max(0, height);
    }

    public synchronized long durationMs() {
        return (capture != null) ? capture.durationMs() : 0L;
     }

     public synchronized long positionMs() {
        return (capture != null) ? capture.positionMs() : 0L;
     }

     public void setProgressListener(ProgressListener l) {
        this.progressListener = l;
     }

     public void setCaptionsEnabled(boolean on) {
        config.captionsEnabled = on;
     }
}
