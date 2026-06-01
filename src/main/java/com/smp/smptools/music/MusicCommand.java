package com.smp.smptools.music;

import com.smp.smptools.SMPTools;
import com.smp.smptools.utils.Constants;
import com.smp.smptools.utils.BoundedInputStream;
import com.smp.smptools.utils.URLValidator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class MusicCommand implements CommandExecutor {

    private final SMPTools plugin;
    private final Map<UUID, SongPlayer> playingTasks = new ConcurrentHashMap<>();

    public MusicCommand(SMPTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /music <play|broadcast|stop> [url]", NamedTextColor.RED));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("broadcast")) {
            if (!player.hasPermission("smptools.music.broadcast")) {
                player.sendMessage(Component.text("You don't have permission to broadcast music.", NamedTextColor.RED));
                return true;
            }
        }

        if (subCommand.equals("stop")) {
            SongPlayer task = playingTasks.remove(player.getUniqueId());
            if (task != null) {
                task.cancel();
                player.sendMessage(Component.text("Music stopped.", NamedTextColor.YELLOW));
            } else {
                player.sendMessage(Component.text("No music is currently playing.", NamedTextColor.RED));
            }
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /music " + subCommand + " <url_or_name>", NamedTextColor.RED));
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
                player.sendMessage(Component.text("The base URL for songs is not configured on the server.", NamedTextColor.RED));
                return true;
            }
            try {
                String encodedName = java.net.URLEncoder.encode(input, java.nio.charset.StandardCharsets.UTF_8.toString());
                String urlPathCompatibleEncodedName = encodedName.replace("+", "%20");
                urlString = baseUrl + urlPathCompatibleEncodedName + ".nbs";
            } catch (java.io.UnsupportedEncodingException e) {
                player.sendMessage(Component.text("An internal error occurred while encoding the song name.", NamedTextColor.RED));
                plugin.getLogger().log(Level.SEVERE, "Failed to encode song name", e);
                return true;
            }
        }

        // Validate URL
        URL url;
        try {
            url = URLValidator.validateAndCreate(urlString);
        } catch (Exception e) {
            player.sendMessage(Component.text("Invalid URL: " + e.getMessage(), NamedTextColor.RED));
            return true;
        }

        player.sendMessage(Component.text("Downloading and parsing song...", NamedTextColor.GRAY));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URLConnection conn = URLValidator.openConnection(url);
                try (InputStream rawStream = conn.getInputStream();
                     InputStream boundedStream = new BoundedInputStream(rawStream, Constants.MAX_NBS_DOWNLOAD_BYTES)) {
                    Song song = NBSParser.parse(boundedStream);
                    if (song == null) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            player.sendMessage(Component.text("Failed to parse the song. Please check the file format.", NamedTextColor.RED));
                        });
                        return;
                    }

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        SongPlayer songPlayer;
                        if (subCommand.equals("broadcast")) {
                            songPlayer = new SongPlayer(song, Bukkit.getOnlinePlayers());
                        } else {
                            songPlayer = new SongPlayer(song, player.getWorld().getPlayers());
                        }
                        
                        playingTasks.put(player.getUniqueId(), songPlayer);
                        songPlayer.play(plugin);
                        player.sendMessage(Component.text("Now playing: " + song.getTitle(), NamedTextColor.GREEN));
                    });
                }
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(Component.text("Failed to download the song. Please check the URL.", NamedTextColor.RED));
                });
                plugin.getLogger().log(Level.SEVERE, "Failed to download song", e);
            }
        });

        return true;
    }
}
