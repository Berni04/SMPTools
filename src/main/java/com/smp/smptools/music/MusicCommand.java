package com.smp.smptools.music;

import com.smp.smptools.SMPTools;
import com.smp.smptools.commands.AbstractPlayerCommand;
import com.smp.smptools.utils.Constants;
import com.smp.smptools.utils.BoundedInputStream;
import com.smp.smptools.utils.URLValidator;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class MusicCommand extends AbstractPlayerCommand {

    private final Map<UUID, SongPlayer> playingTasks = new ConcurrentHashMap<>();

    public MusicCommand(SMPTools plugin) {
        super(plugin);
    }

    @Override
    protected boolean onPlayerCommand(Player player, Command command, String label, String[] args) {

        if (args.length == 0) {
            player.sendMessage(plugin.getMessageManager().getMessage("music.usage"));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("broadcast")) {
            if (!player.hasPermission("smptools.music.broadcast")) {
                player.sendMessage(plugin.getMessageManager().getMessage("music.no-permission-broadcast"));
                return true;
            }
        }

        if (subCommand.equals("stop")) {
            SongPlayer task = playingTasks.remove(player.getUniqueId());
            if (task != null) {
                task.cancel();
                player.sendMessage(plugin.getMessageManager().getMessage("music.stopped"));
            } else {
                player.sendMessage(plugin.getMessageManager().getMessage("music.not-playing"));
            }
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(plugin.getMessageManager().getMessage("common.usage", player, Map.of("usage", "/music " + subCommand + " <url_or_name>")));
            return true;
        }

        // Join all arguments after the subcommand to allow for names with spaces
        StringBuilder inputBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            inputBuilder.append(args[i]).append(" ");
        }
        String input = inputBuilder.toString().trim();
        String urlString;

        if (input.startsWith("http://") || input.startsWith("https://")) {
            urlString = input;
        } else {
            String baseUrl = plugin.getConfig().getString("features.music-player.base-url");
            if (baseUrl == null || baseUrl.isEmpty()) {
                player.sendMessage(plugin.getMessageManager().getMessage("music.base-url-not-configured"));
                return true;
            }
            try {
                String encodedName = java.net.URLEncoder.encode(input, java.nio.charset.StandardCharsets.UTF_8.toString());
                String urlPathCompatibleEncodedName = encodedName.replace("+", "%20");
                urlString = baseUrl + urlPathCompatibleEncodedName + ".nbs";
            } catch (java.io.UnsupportedEncodingException e) {
                player.sendMessage(plugin.getMessageManager().getMessage("music.encoding-error"));
                plugin.getLogger().log(Level.SEVERE, "Failed to encode song name", e);
                return true;
            }
        }

        // Validate URL
        URL url;
        try {
            url = URLValidator.validateAndCreate(urlString);
        } catch (Exception e) {
            player.sendMessage(plugin.getMessageManager().getMessage("music.invalid-url", player, Map.of("error", e.getMessage())));
            return true;
        }

        player.sendMessage(plugin.getMessageManager().getMessage("music.downloading"));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URLConnection conn = URLValidator.openConnection(url);
                try (InputStream rawStream = conn.getInputStream();
                     InputStream boundedStream = new BoundedInputStream(rawStream, Constants.MAX_NBS_DOWNLOAD_BYTES)) {
                    Song song = NBSParser.parse(boundedStream);
                    if (song == null) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            player.sendMessage(plugin.getMessageManager().getMessage("music.parse-failed"));
                        });
                        return;
                    }

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        SongPlayer existing = playingTasks.remove(player.getUniqueId());
                        if (existing != null) {
                            try { existing.cancel(); } catch (Exception ignored) {}
                        }
                        SongPlayer songPlayer;
                        if (subCommand.equals("broadcast")) {
                            songPlayer = new SongPlayer(song, Bukkit.getOnlinePlayers());
                        } else {
                            songPlayer = new SongPlayer(song, player.getWorld().getPlayers());
                        }
                        
                        playingTasks.put(player.getUniqueId(), songPlayer);
                        songPlayer.play(plugin);
                        player.sendMessage(plugin.getMessageManager().getMessage("music.now-playing", player, Map.of("title", song.getTitle())));
                    });
                }
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(plugin.getMessageManager().getMessage("music.download-failed"));
                });
                plugin.getLogger().log(Level.SEVERE, "Failed to download song", e);
            }
        });

        return true;
    }
}
