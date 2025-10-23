package videoapp;

/**
 * Application entry point that initializes OpenCV, applies the system
 * look and feel, and displays the main video player window.
 *
 * @author Glenn Anciado
 * @version 1.0
 */

import videoapp.ui.VideoPlayerFrame;
import nu.pattern.OpenCV;

import javax.swing.*;

public class Driver{
    public static void main(String[] args) throws Exception{
        OpenCV.loadLocally();
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        SwingUtilities.invokeLater(() -> new VideoPlayerFrame().setVisible(true));
    }
}
