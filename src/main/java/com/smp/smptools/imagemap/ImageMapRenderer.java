package com.smp.smptools.imagemap;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.awt.image.BufferedImage;

public class ImageMapRenderer extends MapRenderer {

    private final BufferedImage image;
    private boolean hasRendered = false;

    public ImageMapRenderer(BufferedImage image) {
        this.image = image;
    }

    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        if (hasRendered) {
            return;
        }

        canvas.drawImage(0, 0, image);
        hasRendered = true;
    }
}
