package com.smp.smptools.imagemap;

import org.bukkit.map.MapPalette;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ImageProcessor {

    private static final Logger logger = Logger.getLogger(ImageProcessor.class.getName());

    public static BufferedImage getImage(InputStream inputStream, int width, int height) {
        try {
            BufferedImage downloadedImage = ImageIO.read(inputStream);
            if (downloadedImage == null) {
                return null;
            }
            BufferedImage resizedImage = resizeImage(downloadedImage, width, height);
            return ditherImage(resizedImage);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to process image from input stream", e);
            return null;
        }
    }

    public static BufferedImage getSubImage(BufferedImage fullImage, int x, int y, int width, int height) {
        return fullImage.getSubimage(x, y, width, height);
    }

    private static BufferedImage resizeImage(BufferedImage originalImage, int width, int height) {
        Image resultingImage = originalImage.getScaledInstance(width, height, Image.SCALE_DEFAULT);
        BufferedImage outputImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = outputImage.createGraphics();
        g2d.drawImage(resultingImage, 0, 0, null);
        g2d.dispose();
        return outputImage;
    }

    private static BufferedImage ditherImage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage dithered = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                java.awt.Color color = new java.awt.Color(rgb, true);
                byte colorIndex = MapPalette.matchColor(color);
                dithered.setRGB(x, y, MapPalette.getColor(colorIndex).getRGB());
            }
        }
        return dithered;
    }
}
