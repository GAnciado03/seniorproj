package videoapp.ui;

/**
 * Helper to open a file chooser dialog and instruct the
 * VideoPlayer to play the selected file.
 *
 * @author Glenn Anciado
 * @version 1.0
 */

import videoapp.core.VideoPlayer;

import javax.swing.*;
import java.io.File;
import static java.lang.System.getProperty;

public class VideoChooseHandler {
    private final JFrame parentFrame;
    private final VideoPlayer player;

    public VideoChooseHandler(JFrame parentFrame, VideoPlayer player) {
        this.parentFrame = parentFrame;
        this.player = player;
    }

    public void chooseToPlay() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose a video file.");
        File videos = new File(
            new File(
                new File(
                    new File(
                        new File(getProperty("user.home"), "OneDrive"),
                        "Documents"
                    ),
                    "GitHub"
                ),
                "seniorproj"
            ),
            "jgs-testing-data"
        );
        if(videos.isDirectory()) {
            chooser.setCurrentDirectory(videos);
        }
        int result = chooser.showOpenDialog(parentFrame);
        if(result == JFileChooser.APPROVE_OPTION){
            File file = chooser.getSelectedFile();
            player.play(file.getAbsolutePath());
        }
    }
}
