package com.smp.smptools.music;

import com.smp.smptools.utils.Constants;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NBSParser {

    private static final Logger logger = Logger.getLogger(NBSParser.class.getName());

    public static Song parse(InputStream inputStream) throws MusicParseException {
        try (DataInputStream dataInputStream = new DataInputStream(inputStream)) {
            // Header
            short length = readShort(dataInputStream);
            if (length < 0) {
                throw new MusicParseException("Invalid song length: " + length);
            }

            short songHeight = readShort(dataInputStream);
            if (songHeight < 0 || songHeight > 1000) {
                throw new MusicParseException("Invalid song height: " + songHeight);
            }

            String title = readString(dataInputStream);
            String author = readString(dataInputStream);
            readString(dataInputStream); // Original author
            readString(dataInputStream); // Description

            short speedRaw = readShort(dataInputStream);
            float speed = speedRaw / 100f;
            if (speed < Constants.MIN_SONG_SPEED || speed > Constants.MAX_SONG_SPEED) {
                speed = (float) Math.max(Constants.MIN_SONG_SPEED, Math.min(Constants.MAX_SONG_SPEED, speed));
            }

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
            int maxTicks = length * 2; // Safety limit
            while (true) {
                int jumpTicks = readShort(dataInputStream);
                if (jumpTicks == 0) {
                    break;
                }
                tick += jumpTicks;

                if (tick > maxTicks) {
                    throw new MusicParseException("Song exceeds maximum tick count");
                }

                int layer = -1;
                while (true) {
                    int jumpLayers = readShort(dataInputStream);
                    if (jumpLayers == 0) {
                        break;
                    }
                    layer += jumpLayers;

                    if (layer > songHeight) {
                        throw new MusicParseException("Layer index exceeds song height");
                    }

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

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to parse NBS file", e);
            throw new MusicParseException("Failed to read NBS file: " + e.getMessage(), e);
        } catch (MusicParseException e) {
            throw e;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error parsing NBS file", e);
            throw new MusicParseException("Invalid NBS file format: " + e.getMessage(), e);
        }
    }

    private static short readShort(DataInputStream dataInputStream) throws IOException {
        int byte1 = dataInputStream.readUnsignedByte();
        int byte2 = dataInputStream.readUnsignedByte();
        return (short) (byte1 + (byte2 << 8));
    }

    private static int readInt(DataInputStream dataInputStream) throws IOException {
        int byte1 = dataInputStream.readUnsignedByte();
        int byte2 = dataInputStream.readUnsignedByte();
        int byte3 = dataInputStream.readUnsignedByte();
        int byte4 = dataInputStream.readUnsignedByte();
        return byte1 + (byte2 << 8) + (byte3 << 16) + (byte4 << 24);
    }

    private static String readString(DataInputStream dataInputStream) throws IOException {
        int length = readInt(dataInputStream);
        if (length < 0 || length > Constants.MAX_NBS_STRING_LENGTH) {
            throw new IOException("Invalid string length: " + length);
        }
        byte[] bytes = new byte[length];
        dataInputStream.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
