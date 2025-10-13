package videoapp;

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
