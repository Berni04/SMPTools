package com.smp.smptools.music;

import com.smp.smptools.SMPTools;
import com.smp.smptools.utils.Constants;
import org.bukkit.Instrument;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;

public class SongPlayer extends BukkitRunnable {

    private final Song song;
    private final Collection<? extends Player> audience;
    private int tick = -1;

    public SongPlayer(Song song, Collection<? extends Player> audience) {
        this.song = song;
        this.audience = audience;
    }

    @Override
    public void run() {
        if (tick > song.getLength()) {
            this.cancel();
            return;
        }

        tick++;
        for (Layer layer : song.getLayerMap().values()) {
            Note note = layer.getNoteMap().get(tick);
            if (note == null) {
                continue;
            }

            for (Player player : audience) {
                if (player != null && player.isOnline()) {
                    int noteId = Math.max(0, Math.min(24, note.getKey() - 33));
                    player.playNote(player.getLocation(), getInstrument(note.getInstrument()), new org.bukkit.Note(noteId));
                }
            }
        }
    }

    public void play(SMPTools plugin) {
        double speed = Math.max(Constants.MIN_SONG_SPEED, 
                       Math.min(Constants.MAX_SONG_SPEED, song.getSpeed()));
        long period = Math.max(1, (long) (20 / speed));
        this.runTaskTimer(plugin, 0, period);
    }

    private Instrument getInstrument(byte instrument) {
        switch (instrument) {
            case 0: return Instrument.PIANO;
            case 1: return Instrument.BASS_GUITAR;
            case 2: return Instrument.BASS_DRUM;
            case 3: return Instrument.SNARE_DRUM;
            case 4: return Instrument.STICKS;
            case 5: return Instrument.GUITAR;
            case 6: return Instrument.FLUTE;
            case 7: return Instrument.BELL;
            case 8: return Instrument.CHIME;
            case 9: return Instrument.XYLOPHONE;
            default: return Instrument.PIANO;
        }
    }
}
