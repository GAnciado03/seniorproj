package videoapp.core;

/**
 * Container for subtitle cues providing lookup of the active cue
 * at a given playback timestamp.
 *
 * @author Glenn Anciado
 * @version 1.0
 */

import java.util.ArrayList;
import java.util.List;

public class CaptionsTrack {
    private final List<CaptionCue> cues = new ArrayList<>();
    public void add(CaptionCue cue) {
        cues.add(cue);
    }
    public List<CaptionCue> cues() {
        return cues;
    }

    public CaptionCue activeAt(long tMs) {
        for(CaptionCue c : cues) {
            if (c.contains(tMs)) {
                return c;
            }
        }
        return null;
    }
}
