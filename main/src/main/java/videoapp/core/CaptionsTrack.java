package videoapp.core;

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
