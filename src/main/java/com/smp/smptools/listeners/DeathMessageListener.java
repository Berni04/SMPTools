package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class DeathMessageListener implements Listener {

    private final SMPTools plugin;
    private final Random random = new Random();

    public DeathMessageListener(SMPTools plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        if (plugin.getConfig().getBoolean("features.funny-death-messages.enabled", true)) {
            handleFunnyDeathMessage(event);
        } else {
            // Handle default death message formatting
            Component formattedPlayerName = plugin.getChatManager().getFormattedDisplayName(player);
            Component originalDeathMessage = event.deathMessage();

            if (originalDeathMessage != null) {
                // Use replaceText to replace the player's raw name with the formatted component
                Component finalMessage = originalDeathMessage
                        .replaceText(builder -> builder.matchLiteral(player.getName())
                                .replacement(formattedPlayerName));

                if (player.getKiller() != null) {
                    Component formattedKillerName = plugin.getChatManager().getFormattedDisplayName(player.getKiller());
                    finalMessage = finalMessage
                            .replaceText(builder -> builder.matchLiteral(player.getKiller().getName())
                                    .replacement(formattedKillerName));
                }

                event.deathMessage(finalMessage);
            }
        }
    }

    private void handleFunnyDeathMessage(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Component formattedPlayerName = plugin.getChatManager().getFormattedDisplayName(player);
        String deathMessageTemplate;

        EntityDamageEvent lastDamage = player.getLastDamageCause();
        if (lastDamage == null) {
            deathMessageTemplate = getRandomMessage(Arrays.asList(
                    "ceased to exist.",
                    "went gentle into that good night.",
                    "'s story ends here."));
            event.deathMessage(
                    formattedPlayerName.append(Component.text(" " + deathMessageTemplate, NamedTextColor.RED)));
        } else {
            Component finalMessage;
            switch (lastDamage.getCause()) {
                case ENTITY_ATTACK:
                    if (lastDamage instanceof EntityDamageByEntityEvent) {
                        Entity damager = ((EntityDamageByEntityEvent) lastDamage).getDamager();
                        if (damager instanceof Player) {
                            Component formattedKillerName = plugin.getChatManager()
                                    .getFormattedDisplayName((Player) damager);
                            deathMessageTemplate = getRandomMessage(Arrays.asList(
                                    " was sent back to the lobby by ",
                                    " learned that %killer% is not their friend.",
                                    " was outplayed by "));
                            if (deathMessageTemplate.contains("%killer%")) {
                                finalMessage = formattedPlayerName.append(
                                        Component.text(deathMessageTemplate.replace("%killer%", ""), NamedTextColor.RED)
                                                .append(formattedKillerName));
                            } else {
                                finalMessage = formattedPlayerName.append(Component
                                        .text(deathMessageTemplate, NamedTextColor.RED).append(formattedKillerName));
                            }
                        } else {
                            deathMessageTemplate = getRandomMessage(Arrays.asList(
                                    " was slain by a " + damager.getType().name().toLowerCase() + ".",
                                    " had a bone to pick with a " + damager.getType().name().toLowerCase() + "."));
                            finalMessage = formattedPlayerName
                                    .append(Component.text(deathMessageTemplate, NamedTextColor.RED));
                        }
                    } else {
                        finalMessage = formattedPlayerName
                                .append(Component.text(" was killed by something.", NamedTextColor.RED));
                    }
                    break;
                case ENTITY_EXPLOSION:
                    deathMessageTemplate = getRandomMessage(Arrays.asList(
                            " got a hug from a Creeper.",
                            " learned that some hugs are explosive."));
                    finalMessage = formattedPlayerName.append(Component.text(deathMessageTemplate, NamedTextColor.RED));
                    break;
                case BLOCK_EXPLOSION:
                    deathMessageTemplate = getRandomMessage(Arrays.asList(
                            " should not have slept in the Nether.",
                            "'s bed went boom."));
                    finalMessage = formattedPlayerName.append(Component.text(deathMessageTemplate, NamedTextColor.RED));
                    break;
                case FALL:
                    deathMessageTemplate = getRandomMessage(Arrays.asList(
                            " thought they were a bird.",
                            " forgot to deploy their parachute.",
                            " tested gravity. It still works."));
                    finalMessage = formattedPlayerName.append(Component.text(deathMessageTemplate, NamedTextColor.RED));
                    break;
                case LAVA:
                    deathMessageTemplate = getRandomMessage(Arrays.asList(
                            " tried to swim in the forbidden soup.",
                            " is now one with the magma."));
                    finalMessage = formattedPlayerName.append(Component.text(deathMessageTemplate, NamedTextColor.RED));
                    break;
                case DROWNING:
                    deathMessageTemplate = getRandomMessage(Arrays.asList(
                            " forgot how to breathe.",
                            " is sleeping with the fishes."));
                    finalMessage = formattedPlayerName.append(Component.text(deathMessageTemplate, NamedTextColor.RED));
                    break;
                case VOID:
                    deathMessageTemplate = getRandomMessage(Arrays.asList(
                            " fell out of the world.",
                            " has been deleted from the simulation."));
                    finalMessage = formattedPlayerName.append(Component.text(deathMessageTemplate, NamedTextColor.RED));
                    break;
                case FIRE:
                case FIRE_TICK:
                    deathMessageTemplate = getRandomMessage(Arrays.asList(
                            " is extra crispy now.",
                            " forgot to stop, drop, and roll."));
                    finalMessage = formattedPlayerName.append(Component.text(deathMessageTemplate, NamedTextColor.RED));
                    break;
                default:
                    deathMessageTemplate = getRandomMessage(Arrays.asList(
                            " died in a mysterious way.",
                            " met their end."));
                    finalMessage = formattedPlayerName.append(Component.text(deathMessageTemplate, NamedTextColor.RED));
                    break;
            }
            event.deathMessage(finalMessage);
        }
    }

    private String getRandomMessage(List<String> messages) {
        return messages.get(random.nextInt(messages.size()));
    }
}
