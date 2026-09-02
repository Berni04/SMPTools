package com.smp.smptools.imagemap;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ImageProcessorTest {

    @Test
    void testValidImageDecodes() throws IOException {
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

        BufferedImage result = ImageProcessor.getImage(bais, 128, 128);
        assertNotNull(result);
    }

    @Test
    void testNullInputStreamReturnsNull() {
        assertNull(ImageProcessor.getImage(null, 128, 128));
    }
}
