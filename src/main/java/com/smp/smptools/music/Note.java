package com.smp.smptools.music;

public class Note {
    private final byte instrument;
    private final byte key;

    public Note(byte instrument, byte key) {
        this.instrument = instrument;
        this.key = key;
    }

    public byte getInstrument() {
        return instrument;
    }

    public byte getKey() {
        return key;
    }
}
