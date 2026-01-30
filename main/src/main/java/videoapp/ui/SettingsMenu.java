package videoapp.ui;

import javax.swing.*;
import java.util.function.*;

/**
 * Context menu for runtime playback settings such as speed and effective
 * resolution (via decode percentage) relative to the source video size.
 * Adds check indicators for current speed and resolution and provides
 * overlay sync tools (offset and anchor row).
 *
 * @author Glenn Anciado
 * @version 2.0
 */

public class SettingsMenu extends JPopupMenu{
    public SettingsMenu(double currentSpeed, int currentPercent,
                        DoubleConsumer onSpeed, IntConsumer onPercent,
                        java.util.function.BiConsumer<Integer, Integer> onResolution,
                        int sourceWidthPx, int sourceHeightPx,
                        int videoWidthPx, int videoHeightPx,
                        boolean darkModeEnabled,
                        Consumer<Boolean> onDarkModeToggle) {

        JMenu speed = new JMenu("Playback speed");
        ButtonGroup speedGroup = new ButtonGroup();
        speed.add(speedItem("0.5x", 0.5, currentSpeed, onSpeed, speedGroup));
        speed.add(speedItem("1.0x", 1.0, currentSpeed, onSpeed, speedGroup));
        speed.add(speedItem("1.5x", 1.5, currentSpeed, onSpeed, speedGroup));
        speed.add(speedItem("2.0x", 2.0, currentSpeed, onSpeed, speedGroup));
        add(speed);


        JMenu res = new JMenu("Resolution");
        JMenuItem current = new JMenuItem(currentResolutionLabel(videoWidthPx, videoHeightPx));
        current.setEnabled(false);
        res.add(current);
        add(res);

        addSeparator();
        JCheckBoxMenuItem darkMode = new JCheckBoxMenuItem("Dark mode", darkModeEnabled);
        darkMode.addActionListener(e -> {
            if (onDarkModeToggle != null) {
                onDarkModeToggle.accept(darkMode.isSelected());
            }
        });
        add(darkMode);
    }

    private String currentResolutionLabel(int w, int h) {
        if (w > 0 && h > 0) {
            return "Current: " + w + "x" + h;
        }
        return "Current resolution unavailable";
    }

    private JMenuItem speedItem(String label, double speedValue, double current,
                                DoubleConsumer onSpeed, ButtonGroup group) {
        boolean selected = Math.abs(speedValue - current) < 1e-9;
        JRadioButtonMenuItem it = new JRadioButtonMenuItem(label, selected);
        it.addActionListener(e -> onSpeed.accept(speedValue));
        if (group != null) group.add(it);
        return it;
    }
}
