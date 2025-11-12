package videoapp.ui;

import videoapp.core.VideoPlayer;

import javax.swing.*;
import java.io.File;
import static videoapp.util.ChooserUtils.*;

/**
 * Helper to open a file chooser dialog and instruct the
 * VideoPlayer to play the selected file.
 *
 * @author Glenn Anciado
 * @version 1.0
 */

public class VideoChooseHandler {
    private final JFrame parentFrame;
    private final VideoPlayer player;

    public VideoChooseHandler(JFrame parentFrame, VideoPlayer player) {
        this.parentFrame = parentFrame;
        this.player = player;
    }

    public boolean chooseToPlay() {
        return chooseToPlay(null);
    }

    public boolean chooseToPlay(File startDir) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose a video file.");
        File initial = (startDir != null) ? startDir : initialVideoDir();
        if (initial != null) chooser.setCurrentDirectory(initial);
        int result = chooser.showOpenDialog(parentFrame);
        if(result == JFileChooser.APPROVE_OPTION){
            File file = chooser.getSelectedFile();
            rememberVideoSelection(file);
            player.play(file.getAbsolutePath());
            return true;
        }
        return false;
    }
}
