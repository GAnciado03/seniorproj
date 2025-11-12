package videoapp.core;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

/**
 * Thin wrapper around OpenCV VideoCapture providing open/close,
 * frame retrieval, timing and dimension queries, and millisecond seeking.
 *
 * @author Glenn Anciado
 * @version 1.0
 */

public class VideoSource {
    private VideoCapture capture;

    public boolean open(String path) {
        close();
        capture = new VideoCapture(path);
        return capture.isOpened();
    }
    public boolean grab() {
        return capture != null && capture.grab();
    }
    public boolean retrieve(Mat out) {
        return capture != null && capture.retrieve(out);
    }
    public boolean read(Mat out) {
        return capture != null && capture.read(out);
    }

    /**
     * Frames per second reported by the source (may be 0 if unknown).
     */
    public double fps() {
        return (capture != null) ? capture.get(Videoio.CAP_PROP_FPS) : 0.0;
    }
    public long frameCount() {
        return (capture != null) ? (long) capture.get(Videoio.CAP_PROP_FRAME_COUNT) : 0L;
    }
    public long positionMs() {
        return (capture != null) ? (long) capture.get(Videoio.CAP_PROP_POS_MSEC) : 0L;
    }
    public long durationMs() {
        double fps = fps(); long frames = frameCount();
        if(fps <= 0 || frames <= 0) {
            return 0L;
        }
        return (long) Math.round((frames / fps) * 1000.0);
    }

    public boolean seekMs(long ms) {
        return capture != null && capture.set(Videoio.CAP_PROP_POS_MSEC, ms);
    }

    public int width() {
        return (capture != null) ? (int) capture.get(Videoio.CAP_PROP_FRAME_WIDTH) : 0;
    }
    public int height() {
        return (capture != null) ? (int) capture.get(Videoio.CAP_PROP_FRAME_HEIGHT) : 0;
    }

    public void close() {
        if (capture != null) {
            try {
                capture.release();
            } catch (Exception e) {
                e = null;
            }
        }
        capture = null;
    }
}
