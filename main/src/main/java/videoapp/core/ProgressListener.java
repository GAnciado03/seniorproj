package videoapp.core;

public interface ProgressListener {
    void onProgress(long posMs, long durationMs);
}
