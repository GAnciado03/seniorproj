package videoapp.ui;

import videoapp.core.VideoPlayer;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class SettingsMenu extends JPopupMenu{
    public SettingsMenu(VideoPlayer player, Runnable refreshUi) {
        JMenu speed = new JMenu("Playback speed");
        speed.add(item("0.5x", () -> player.setSpeed(0.5), refreshUi));
        speed.add(item("1.0x", () -> player.setSpeed(1.0), refreshUi));
        speed.add(item("1.5x", () -> player.setSpeed(1.5), refreshUi));
        speed.add(item("2.0x", () -> player.setSpeed(2.0), refreshUi));
        add(speed);

        JMenu res = new JMenu("Resolution");
        res.add(item("Source (native)", () -> player.setTargetSize(0, 0), refreshUi));
        res.add(item("1080p (1920x1080)", () -> player.setTargetSize(1920, 1080), refreshUi));
        res.add(item("720p (1280x720)", () -> player.setTargetSize(1280, 720), refreshUi));
        res.add(item("480p (854x480)", () -> player.setTargetSize(854, 480), refreshUi));
        res.add(item("360p (640x360))", () -> player.setTargetSize(640, 360), refreshUi));
        add(res);
    }

    private JMenuItem item(String label, Runnable apply, Runnable refresh) {
        return new JMenuItem(new AbstractAction(label){
            @Override
            public void actionPerformed(ActionEvent e) {
                apply.run();
                if(refresh != null) {
                    refresh.run();
                }
            }
        });
    }
}
