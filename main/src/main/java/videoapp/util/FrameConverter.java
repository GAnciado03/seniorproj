package videoapp.util;

/**
 * Utilities to convert OpenCV Mat frames into
 * Java BufferedImages instances, handling common
 * channel formats and color conversions.
 *
 * @author Glenn Anciado
 * @version 1.0
 */

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

public final class FrameConverter {
    private FrameConverter(){}

    public static BufferedImage matToBufferedImage(Mat mat) {
        int width = mat.cols(), height = mat.rows(), channels = mat.channels();

        if(channels == 3) {
            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
            byte[] dst = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
            mat.get(0, 0, dst);
            return img;
        } else if (channels == 1) {
            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
            byte[] dst = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
            mat.get(0, 0, dst);
            return img;
        } else {
            Mat bgr = new Mat();
            Imgproc.cvtColor(mat, bgr, Imgproc.COLOR_BGRA2BGR);
            try{
                return matToBufferedImage(bgr);
            } finally {
                bgr.release();
            }
        }
    }
}
