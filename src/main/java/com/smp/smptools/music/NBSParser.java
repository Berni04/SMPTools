package com.smp.smptools.music;

import java.io.DataInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class NBSParser {

    public static Song parse(InputStream inputStream) {
        try (DataInputStream dataInputStream = new DataInputStream(inputStream)) {
            // Header
            short length = readShort(dataInputStream);
            short songHeight = readShort(dataInputStream);
            String title = readString(dataInputStream);
            String author = readString(dataInputStream);
            readString(dataInputStream); // Original author
            readString(dataInputStream); // Description
            float speed = readShort(dataInputStream) / 100f;
            dataInputStream.readBoolean(); // auto-save
            dataInputStream.readByte(); // auto-save duration
            dataInputStream.readByte(); // time signature
            readInt(dataInputStream); // minutes spent
            readInt(dataInputStream); // left clicks
            readInt(dataInputStream); // right clicks
            readInt(dataInputStream); // blocks added
            readInt(dataInputStream); // blocks removed
            readString(dataInputStream); // midi/schematic file name

            Song song = new Song(length, songHeight, title, author, speed);

            // Note Blocks
            int tick = -1;
            while (true) {
                int jumpTicks = readShort(dataInputStream);
                if (jumpTicks == 0) {
                    break;
                }
                tick += jumpTicks;

                int layer = -1;
                while (true) {
                    int jumpLayers = readShort(dataInputStream);
                    if (jumpLayers == 0) {
                        break;
                    }
                    layer += jumpLayers;
                    byte instrument = dataInputStream.readByte();
                    byte key = dataInputStream.readByte();
                    song.getLayerMap().computeIfAbsent(layer, k -> new Layer())
                        .getNoteMap().put(tick, new Note(instrument, key));
                }
            }

            // Layers
            if (dataInputStream.available() > 0) {
                for (int i = 0; i < songHeight; i++) {
                    Layer layer = song.getLayerMap().get(i);
                    if (layer == null) {
                        layer = new Layer();
                        song.getLayerMap().put(i, layer);
                    }
                    layer.setName(readString(dataInputStream));
                    layer.setVolume(dataInputStream.readByte());
                }
            }

            return song;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static short readShort(DataInputStream dataInputStream) throws Exception {
        int byte1 = dataInputStream.readUnsignedByte();
        int byte2 = dataInputStream.readUnsignedByte();
        return (short) (byte1 + (byte2 << 8));
    }

    private static int readInt(DataInputStream dataInputStream) throws Exception {
        int byte1 = dataInputStream.readUnsignedByte();
        int byte2 = dataInputStream.readUnsignedByte();
        int byte3 = dataInputStream.readUnsignedByte();
        int byte4 = dataInputStream.readUnsignedByte();
        return byte1 + (byte2 << 8) + (byte3 << 16) + (byte4 << 24);
    }

    private static String readString(DataInputStream dataInputStream) throws Exception {
        int length = readInt(dataInputStream);
        byte[] bytes = new byte[length];
        dataInputStream.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
