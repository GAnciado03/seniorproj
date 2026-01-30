package videoapp.ui;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * Immutable container for overlay points and precomputed heatmap data loaded from CSV.
 *
 * @author Glenn Anciado
 * @version 1.0
 */

public record OverlayPayload(
        File source,
        List<OverlayPoint> staticPoints,
        List<TimedOverlayPoint> timedPoints,
        int[][] heatmapGrid,
        int heatmapMaxCount,
        boolean timed,
        String message) {

    public static OverlayPayload staticPayload(File source,
                                               List<OverlayPoint> points,
                                               int[][] grid,
                                               int maxCount,
                                               String message) {
        return new OverlayPayload(
                source,
                List.copyOf(points),
                Collections.emptyList(),
                grid,
                maxCount,
                false,
                message
        );
    }

    public static OverlayPayload timedPayload(File source,
                                              List<TimedOverlayPoint> points,
                                              int[][] grid,
                                              int maxCount,
                                              String message) {
        return new OverlayPayload(
                source,
                Collections.emptyList(),
                List.copyOf(points),
                grid,
                maxCount,
                true,
                message
        );
    }
}
