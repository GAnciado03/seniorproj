package videoapp.ui;

import videoapp.core.VideoPlayer;
import videoapp.ui.VideoPanelRenderer.ScalingMode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.concurrent.atomic.AtomicInteger;

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

        final VideoPlayer player = new VideoPlayer(videoPanel);
        final AtomicInteger decodePercent = new AtomicInteger(100);

        final Timer resizeDebounce = new Timer(140, e-> {
            int pw = Math.max(1, videoPanel.getWidth());
            int ph = Math.max(1, videoPanel.getHeight());

            double scale = Math.max(0.1, decodePercent.get() / 100.0);
            int tw = (int) Math.round(pw * scale);
            int th = (int) Math.round(ph * scale);

            if(tw < 160) tw = 160; else if (tw > 3840) tw = 3840;
            if((tw & 1) != 0) tw--;
            if(th < 90) th = 90; else if (th > 2160) th = 2160;
            if((th & 1) != 0) th--;

            player.setTargetSize(tw, th);
            videoPanel.requestPackOnNextFrame();

        });
        resizeDebounce.setRepeats(false);

        videoPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if(resizeDebounce.isRunning()) {
                    resizeDebounce.restart();
                } else {
                    resizeDebounce.start();
                }
            }
        });

        final VideoChooseHandler chooserHandler = new VideoChooseHandler(this, player);

        openBtn.addActionListener(e -> chooserHandler.chooseToPlay());
        stopBtn.addActionListener(e -> player.stop());

        setMinimumSize(new Dimension(600,400));
        pack();
        setLocationRelativeTo(null);

        resizeDebounce.start();
    }
}
