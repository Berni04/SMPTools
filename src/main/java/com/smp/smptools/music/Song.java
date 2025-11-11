package com.smp.smptools.music;

import java.util.HashMap;
import java.util.Map;

public class Song {
    private final Map<Integer, Layer> layerMap = new HashMap<>();
    private final short songHeight;
    private final short length;
    private final String title;
    private final String author;
    private final float speed;

    public Song(short length, short songHeight, String title, String author, float speed) {
        this.length = length;
        this.songHeight = songHeight;
        this.title = title;
        this.author = author;
        this.speed = speed;
    }

    public Map<Integer, Layer> getLayerMap() {
        return layerMap;
    }

    public short getSongHeight() {
        return songHeight;
    }

    public short getLength() {
        return length;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public float getSpeed() {
        return speed;
    }
}
