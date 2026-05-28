package a88.jbay.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ImageProcessorTest {

    @Test
    void testCompressToBytesNullFile() {
        assertNull(ImageProcessor.compressToBytes(null));
    }

    @Test
    void testBytesToImageNullData() {
        assertNull(ImageProcessor.bytesToImage(null));
    }

    @Test
    void testBytesToImageEmptyData() {
        assertNull(ImageProcessor.bytesToImage(new byte[0]));
    }

    @Test
    void testCompressToBytesNonImageFile(@TempDir Path tempDir) throws IOException {
        byte[] content = new byte[600 * 1024];
        for (int i = 0; i < content.length; i++) {
            content[i] = (byte) (i & 0xFF);
        }
        File txtFile = tempDir.resolve("test.txt").toFile();
        try (FileOutputStream fos = new FileOutputStream(txtFile)) {
            fos.write(content);
        }
        assertNull(ImageProcessor.compressToBytes(txtFile));
    }

    @Test
    void testCompressToBytesSmallFile(@TempDir Path tempDir) throws IOException {
        byte[] content = new byte[1024];
        for (int i = 0; i < content.length; i++) {
            content[i] = (byte) (i & 0xFF);
        }
        File smallFile = tempDir.resolve("small.bin").toFile();
        try (FileOutputStream fos = new FileOutputStream(smallFile)) {
            fos.write(content);
        }
        byte[] result = ImageProcessor.compressToBytes(smallFile);
        assertNotNull(result);
        assertArrayEquals(content, result);
    }

    @Test
    void testCompressToBytesFileDoesNotExist() {
        File nonExistent = new File("/nonexistent/path/image.jpg");
        assertNull(ImageProcessor.compressToBytes(nonExistent));
    }
}
