package com.smp.smptools.imagemap;

import org.bukkit.map.MapPalette;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.URL;

public class ImageProcessor {

    public static BufferedImage getImage(URL url) {
        try {
            BufferedImage downloadedImage = ImageIO.read(url);
            if (downloadedImage == null) {
                return null;
            }
            BufferedImage resizedImage = resizeImage(downloadedImage);
            return ditherImage(resizedImage);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static BufferedImage resizeImage(BufferedImage originalImage) {
        Image resultingImage = originalImage.getScaledInstance(128, 128, Image.SCALE_DEFAULT);
        BufferedImage outputImage = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = outputImage.createGraphics();
        g2d.drawImage(resultingImage, 0, 0, null);
        g2d.dispose();
        return outputImage;
    }

    private static BufferedImage ditherImage(BufferedImage image) {
        BufferedImage dithered = new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 128; y++) {
            for (int x = 0; x < 128; x++) {
                int rgb = image.getRGB(x, y);
                java.awt.Color color = new java.awt.Color(rgb, true);
                byte colorIndex = MapPalette.matchColor(color);
                dithered.setRGB(x, y, MapPalette.getColor(colorIndex).getRGB());
            }
        }
        return dithered;
    }
}
