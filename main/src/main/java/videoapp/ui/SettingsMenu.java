package videoapp.ui;

/**
 * Context menu for runtime playback settings such as speed and effective
 * resolution (via decode percentage) relative to the panel size.
 *
 * @author Glenn Anciado
 * @version 1.0
 */

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.function.*;

public class SettingsMenu extends JPopupMenu{
    public SettingsMenu(DoubleConsumer onSpeed, IntConsumer onPercent,
                        int panelWidthPx, int panelHeightPx) {
        JMenu speed = new JMenu("Playback speed");
        speed.add(item("0.5x", () -> onSpeed.accept(0.5)));
        speed.add(item("1.0x", () -> onSpeed.accept(1.0)));
        speed.add(item("1.5x", () -> onSpeed.accept(1.5)));
        speed.add(item("2.0x", () -> onSpeed.accept(2.0)));
        add(speed);

        JMenu res = new JMenu("Resolution");
        res.add(resItem("1920x1080", 1920, 1080, panelWidthPx, panelHeightPx, onPercent));
        res.add(resItem("1280x720", 1280, 720, panelWidthPx, panelHeightPx, onPercent));
        res.add(resItem("854x480", 854, 480, panelWidthPx, panelHeightPx, onPercent));
        res.add(resItem("640x360", 640, 360, panelWidthPx, panelHeightPx, onPercent));
        add(res);
    }

    private JMenuItem item(String label, Runnable action, Runnable after) {
        return new JMenuItem(new AbstractAction(label){
            @Override
            public void actionPerformed(ActionEvent e) {
                if(action != null) action.run();
                if(after != null) after.run();
            }
        });
    }

    private JMenuItem item(String label, Runnable action) {
        return item(label, action, null);
    }

    private JMenuItem resItem(String label, int targetW, int targetH,
                        int panelW, int panelH, IntConsumer onPercent) {
        return item(label, () -> {
            if (panelW <= 0 || panelH <= 0) return;
            double pctW = (targetW / (double) panelW) * 100.0;
            double pctH = (targetH / (double) panelH) * 100.0;
            int pct = (int) Math.round(Math.min(pctW, pctH));
            onPercent.accept(clamp(pct, 10, 400));
        });
    }

    private static int clamp(int v, int min, int max){
        return Math.max(min, Math.min(max, v));
    }
}
