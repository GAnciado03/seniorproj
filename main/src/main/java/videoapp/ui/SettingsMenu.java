package videoapp.ui;

/**
 * Context menu for runtime playback settings such as speed and effective
 * resolution (via decode percentage) relative to the panel size.
 * Adds check indicators for current speed and resolution and provides
 * overlay sync tools (offset and anchor row).
 *
 * @author Glenn Anciado
 * @version 2.0
 */

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.function.*;

public class SettingsMenu extends JPopupMenu{
    /**
     * Builds a settings popup with checked playback-speed options and common
     * resolution presets. The current speed and an approximated percent for
     * each preset determine the selected radio items. Callbacks update the
     * player speed and effective decode percent when a user chooses an item.
     *
     * @author Glenn Anciado
     * @version 2.0
     */
    public SettingsMenu(double currentSpeed, int currentPercent,
                        DoubleConsumer onSpeed, IntConsumer onPercent,
                        int panelWidthPx, int panelHeightPx) {
        
        JMenu speed = new JMenu("Playback speed");
        ButtonGroup speedGroup = new ButtonGroup();
        speed.add(speedItem("0.5x", 0.5, currentSpeed, onSpeed, speedGroup));
        speed.add(speedItem("1.0x", 1.0, currentSpeed, onSpeed, speedGroup));
        speed.add(speedItem("1.5x", 1.5, currentSpeed, onSpeed, speedGroup));
        speed.add(speedItem("2.0x", 2.0, currentSpeed, onSpeed, speedGroup));
        add(speed);

        
        JMenu res = new JMenu("Resolution");
        ButtonGroup resGroup = new ButtonGroup();
        res.add(resItem("1920x1080", 1920, 1080, panelWidthPx, panelHeightPx, currentPercent, onPercent, resGroup));
        res.add(resItem("1280x720", 1280, 720, panelWidthPx, panelHeightPx, currentPercent, onPercent, resGroup));
        res.add(resItem("854x480", 854, 480, panelWidthPx, panelHeightPx, currentPercent, onPercent, resGroup));
        res.add(resItem("640x360", 640, 360, panelWidthPx, panelHeightPx, currentPercent, onPercent, resGroup));
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
                        int panelW, int panelH, int currentPercent, IntConsumer onPercent, ButtonGroup group) {
        int pct = computePercent(targetW, targetH, panelW, panelH);
        boolean selected = Math.abs(pct - currentPercent) <= 2;
        JRadioButtonMenuItem it = new JRadioButtonMenuItem(label, selected);
        it.addActionListener(e -> {
            if (panelW <= 0 || panelH <= 0) return;
            onPercent.accept(clamp(pct, 10, 400));
        });
        if (group != null) group.add(it);
        return it;
    }

    private static int computePercent(int targetW, int targetH, int panelW, int panelH) {
        double pctW = (targetW / (double) panelW) * 100.0;
        double pctH = (targetH / (double) panelH) * 100.0;
        return (int) Math.round(Math.min(pctW, pctH));
    }

    private JMenuItem speedItem(String label, double speedValue, double current,
                                DoubleConsumer onSpeed, ButtonGroup group) {
        boolean selected = Math.abs(speedValue - current) < 1e-9;
        JRadioButtonMenuItem it = new JRadioButtonMenuItem(label, selected);
        it.addActionListener(e -> onSpeed.accept(speedValue));
        if (group != null) group.add(it);
        return it;
    }

    private static int clamp(int v, int min, int max){
        return Math.max(min, Math.min(max, v));
    }
}
