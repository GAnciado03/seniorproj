package videoapp.ui;

import videoapp.core.VideoPlayer;
import videoapp.ui.VideoPanelRenderer.ScalingMode;

import javax.swing.*;
import java.awt.*;

public class VideoPlayerFrame extends JFrame{
    public VideoPlayerFrame() {
        super("Video Player");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        VideoPanelRenderer videoPanel = new VideoPanelRenderer();
        videoPanel.setMode(ScalingMode.AUTO);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        final JButton openBtn = new JButton("Open Video...");
        final JButton stopBtn = new JButton("Stop");
        controls.add(openBtn);
        controls.add(stopBtn);

        add(videoPanel, BorderLayout.CENTER);
        add(controls, BorderLayout.SOUTH);

        VideoPlayer player = new VideoPlayer(videoPanel);
        VideoChooseHandler chooserHandler = new VideoChooseHandler(this, player);

        openBtn.addActionListener(e -> chooserHandler.chooseToPlay());
        stopBtn.addActionListener(e -> player.stop());

        pack();
        setLocationRelativeTo(null);
    }
}
