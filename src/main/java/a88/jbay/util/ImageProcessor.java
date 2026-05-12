package a88.jbay.util;

import javafx.scene.image.Image;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

public class ImageProcessor {

    public static byte[] compressToBytes(File file) {
        if (file == null) return null;

        // Nếu ảnh đã nhỏ hơn 500KB rồi thì không cần nén
        if (file.length() <= 500 * 1024) {
            try (FileInputStream fis = new FileInputStream(file)) {
                return fis.readAllBytes();
            } catch (IOException e) { return null; }
        }

        try {
            BufferedImage originalImage = ImageIO.read(file);
            if (originalImage == null) {
                System.err.println("This image type is not supported.");
                return null;
            }

            // Tạo một bản copy để nén, giữ nguyên bản gốc để resize không bị dồn toa
            BufferedImage workingImage = originalImage;
            double scale = 0.9;
            byte[] imageBytes;

            do {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                // Fix lỗi mất màu/trong suốt: Chuyển sang ảnh RGB chuẩn trước khi lưu JPG
                BufferedImage finalImage = new BufferedImage(workingImage.getWidth(), workingImage.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D graphic = finalImage.createGraphics();
                // Đổ nền trắng cho các vùng trong suốt của PNG để ảnh trông đẹp hơn
                graphic.drawImage(workingImage, 0, 0, Color.WHITE, null);
                graphic.dispose();

                ImageIO.write(finalImage, "jpg", baos);
                imageBytes = baos.toByteArray();

                if (imageBytes.length > 500 * 1024) {
                    // LUÔN RESIZE TỪ ẢNH GỐC để đảm bảo chất lượng
                    workingImage = resizeImage(originalImage, scale);
                    scale -= 0.1;
                }
            } while (imageBytes.length > 500 * 1024 && scale > 0.1);

            return imageBytes;
        } catch (IOException e) {
            e.printStackTrace();
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
