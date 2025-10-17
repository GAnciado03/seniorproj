import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;

import static org.bytedeco.opencv.global.opencv_videoio.*;

/**
 * A simple video player using Swing and JavaCV (OpenCV).
 * Place a video file (e.g., 01.mp4) in src/main/resources to test.
 *
 * @author javiergs
 * @version 1.0
 */
public class Player extends JFrame {
	
	private boolean running = true;
	
	public static void main(String[] args) {
		Player p = new Player("/01.mp4");
		p.setTitle("Swing Video Player");
		p.setSize(960, 540);
		p.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		p.setVisible(true);
	}
	
	public Player(String path) {
		JLabel videoLabel = new JLabel();
		videoLabel.setHorizontalAlignment(SwingConstants.CENTER);
		videoLabel.setVerticalAlignment(SwingConstants.CENTER);
		setLayout(new BorderLayout());
		add(videoLabel, BorderLayout.CENTER);
		
		
		setLocationByPlatform(true);
		setVisible(true);
		new Thread(() -> play(path, videoLabel)).start();
		addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosed(java.awt.event.WindowEvent e) {
				running = false;
			}
		});
	}
	
	private void play(String resourcePath, JLabel label) {
		try {
			String videoPath = resolveResourceToPath(resourcePath);
			VideoCapture cap = new VideoCapture(videoPath, CAP_ANY);
			if (!cap.isOpened()) {
				cap.release();
				cap = new VideoCapture(videoPath, CAP_FFMPEG);
			}
			if (!cap.isOpened()) {
				showError("Cannot open video: " + videoPath);
				return;
			}
			double fps = cap.get(CAP_PROP_FPS);
			if (fps <= 0 || Double.isNaN(fps)) fps = 30.0;
			long frameDelayMs = Math.max(1, Math.round(1000.0 / fps));
			Mat mat = new Mat();
			while (running && cap.read(mat) && !mat.empty()) {
				BufferedImage img = matToBufferedImage(mat);
				SwingUtilities.invokeLater(() -> {
					int w = label.getWidth();
					int h = label.getHeight();
					if (w > 0 && h > 0) {
						Image scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
						label.setIcon(new ImageIcon(scaled));
					} else {
						label.setIcon(new ImageIcon(img));
					}
				});
				Thread.sleep(frameDelayMs);
			}
			cap.release();
		} catch (Exception ex) {
			ex.printStackTrace();
			showError(ex.getMessage());
		}
	}
	
	private static BufferedImage matToBufferedImage(Mat mat) {
		if (mat == null || mat.empty()) return null;
		int width = mat.cols(), height = mat.rows(), channels = mat.channels();
		BufferedImage image;
		if (channels == 1) {
			image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		} else {
			image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		}
		byte[] target = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		mat.data().get(target);
		return image;
	}
	
	private static String resolveResourceToPath(String resourcePath) throws Exception {
		URL url = Player.class.getResource(resourcePath);
		if (url == null) throw new FileNotFoundException("Resource not found: " + resourcePath);
		if ("file".equalsIgnoreCase(url.getProtocol())) {
			return new File(url.toURI()).getAbsolutePath();
		}
		File tmp = Files.createTempFile("video_", ".mp4").toFile();
		tmp.deleteOnExit();
		try (InputStream in = Player.class.getResourceAsStream(resourcePath);
				 OutputStream out = new FileOutputStream(tmp)) {
			in.transferTo(out);
		}
		return tmp.getAbsolutePath();
	}
	
	private static void showError(String msg) {
		SwingUtilities.invokeLater(() ->
			JOptionPane.showMessageDialog(null, msg, "Error", JOptionPane.ERROR_MESSAGE));
	}
	
}