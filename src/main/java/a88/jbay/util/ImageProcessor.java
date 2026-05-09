package a88.jbay.util;

import javafx.scene.image.Image;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

public class ImageProcessor {

    // Chuyển File ảnh thành byte[] và nén xuống dưới 500KB
    public static byte[] compressToBytes(File file) {
        if (file == null) return null;

        try {
            BufferedImage originalImage = ImageIO.read(file);
            double quality = 1.0;
            byte[] imageBytes;

            // Vòng lặp nén: Nếu ảnh > 500KB, giảm kích thước ảnh xuống
            do {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                // Nếu ảnh quá lớn, ta sẽ resize nó (giảm 10% mỗi lần)
                if (quality < 1.0) {
                    originalImage = resizeImage(originalImage, quality);
                }

                // Ghi ảnh vào stream dưới định dạng JPG để nhẹ nhất
                ImageIO.write(originalImage, "jpg", baos);
                imageBytes = baos.toByteArray();

                // Giảm hệ số tỉ lệ cho vòng lặp tiếp theo nếu vẫn > 500KB
                quality -= 0.1;

            } while (imageBytes.length > 500 * 1024 && quality > 0.1);

            return imageBytes;
        } catch (IOException e) {
            System.err.println("Error in processing image: " + e.getMessage());
            return null;
        }
    }

    // Chuyển byte[] từ Server về lại Image để hiển thị trên JavaFX
    public static Image bytesToImage(byte[] data) {
        if (data == null || data.length == 0) return null;
        return new Image(new ByteArrayInputStream(data));
    }

    // Hàm phụ trợ để thay đổi kích thước ảnh
    private static BufferedImage resizeImage(BufferedImage originalImage, double scale) {
        int width = (int) (originalImage.getWidth() * scale);
        int height = (int) (originalImage.getHeight() * scale);

        BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, width, height, null);
        g.dispose();

        return resizedImage;
    }
}
