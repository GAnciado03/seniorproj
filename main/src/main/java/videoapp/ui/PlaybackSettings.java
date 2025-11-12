package videoapp.ui;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

/**
 * Singleton for globally accessible playback settings such as speed and
 * decode percentage, with listener notifications on change.
 *
 * @author Glenn Anciado
 * @version 1.0
 */

public class PlaybackSettings {
    private static final PlaybackSettings INSTANCE = new PlaybackSettings();
    public static PlaybackSettings get() {
        return INSTANCE;
    }
    private PlaybackSettings() {}

    private volatile double speed = 1.0;
    private volatile int decodePercent = 100;

    private final CopyOnWriteArrayList<BiConsumer<Double, Integer>> listeners =
        new CopyOnWriteArrayList<>();

    public double speed() {return speed;}
    public int decodePercent() {return decodePercent;}

    public void setSpeed(double s) {
        double v = Math.max(0.1, Math.min(4.0, s));
        speed =  v;
        notifyListeners();
    }

    public void setDecodePercent(int pct) {
        int v = Math.max(10, Math.min(400, pct));
        decodePercent = v;
        notifyListeners();
    }

    private void onChange(BiConsumer<Double, Integer> l) {
        if (l != null) {
            listeners.add(l);
        }
    }

    private void notifyListeners() {
        for(var l : listeners) {
            l.accept(speed, decodePercent);
        }
    }
}
