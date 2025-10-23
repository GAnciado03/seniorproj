package videoapp.core;

/**
 * Represents a single subtitle cue with start/end times (ms) and
 * display text, and utility to test temporal containment.
 *
 * @author Glenn Anciado
 * @version 1.0
 */

public class CaptionCue {
    public final long startMs;
    public final long endMs;
    public final String text;

    public CaptionCue(long startMs, long endMs, String text) {
        this.startMs = startMs;
        this.endMs = endMs;
        this.text = text;
    }

    public boolean contains(long tMs) {
        return tMs >= startMs && tMs <= endMs;
    }
}
