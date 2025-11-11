package com.smp.smptools.music;

import java.util.HashMap;

public class Layer {
    private final HashMap<Integer, Note> noteMap = new HashMap<>();
    private String name;
    private byte volume;

    public Layer() {
        this.name = "";
        this.volume = 100;
    }

    public HashMap<Integer, Note> getNoteMap() {
        return noteMap;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte getVolume() {
        return volume;
    }

    public void setVolume(byte volume) {
        this.volume = volume;
    }
}