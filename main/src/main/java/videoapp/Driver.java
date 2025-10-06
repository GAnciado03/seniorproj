package videoapp;

import videoapp.ui.VideoPlayerFrame;
import videoapp.util.OpenCvHelpers;

import javax.swing.*;

public class Driver{
    public static void main(String[] args) {
        OpenCvHelpers.load();
        SwingUtilities.invokeLater(() -> new VideoPlayerFrame().setVisible(true));
    }
}
