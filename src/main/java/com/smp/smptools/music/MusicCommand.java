package com.smp.smptools.music;

import com.smp.smptools.SMPTools;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MusicCommand implements CommandExecutor {

    private final SMPTools plugin;
    private final Map<UUID, SongPlayer> playingTasks = new HashMap<>();

    public MusicCommand(SMPTools plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /music <play|broadcast|stop> [url]");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        if (subCommand.equals("stop")) {
            SongPlayer task = playingTasks.remove(player.getUniqueId());
            if (task != null) {
                task.cancel();
                player.sendMessage(ChatColor.YELLOW + "Music stopped.");
            } else {
                player.sendMessage(ChatColor.RED + "No music is currently playing.");
            }
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /music " + subCommand + " <url_or_name>");
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
                player.sendMessage(ChatColor.RED + "The base URL for songs is not configured on the server.");
                return true;
            }
            try {
                // URL encode the filename to handle spaces and other special characters
                String encodedName = java.net.URLEncoder.encode(input, java.nio.charset.StandardCharsets.UTF_8.toString());
                // Replace '+' with '%20' for compatibility with raw file paths
                String urlPathCompatibleEncodedName = encodedName.replace("+", "%20");
                urlString = baseUrl + urlPathCompatibleEncodedName + ".nbs";
            } catch (java.io.UnsupportedEncodingException e) {
                // This should never happen with UTF-8
                player.sendMessage(ChatColor.RED + "An internal error occurred while encoding the song name.");
                e.printStackTrace();
                return true;
            }
        }

        player.sendMessage(ChatColor.GRAY + "Downloading and parsing song...");

        new Thread(() -> {
            try (InputStream stream = new URL(urlString).openStream()) {
                Song song = NBSParser.parse(stream);
                if (song == null) {
                    player.sendMessage(ChatColor.RED + "Failed to parse the song. Please check the file format.");
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
                    player.sendMessage(ChatColor.GREEN + "Now playing: " + song.getTitle());
                });

            } catch (Exception e) {
                player.sendMessage(ChatColor.RED + "Failed to download the song. Please check the URL.");
                e.printStackTrace();
            }
        }).start();

        return true;
    }
}
