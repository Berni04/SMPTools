package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
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

    @EventHandler(priority = EventPriority.LOWEST)
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
            event.deathMessage(formatDeathMessage(formattedPlayerName, deathMessageTemplate));
        } else {
            Component finalMessage;
            switch (lastDamage.getCause()) {
                case ENTITY_ATTACK:
                case ENTITY_SWEEP_ATTACK:
                case PROJECTILE:
                    if (lastDamage instanceof EntityDamageByEntityEvent damageEvent) {
                        Entity damager = damageEvent.getDamager();
                        if (damager instanceof Projectile projectile) {
                            if (projectile.getShooter() instanceof Entity shooterEntity) {
                                damager = shooterEntity;
                            }
                        }

                        Player killer = (damager instanceof Player p) ? p : null;
                        if (killer != null) {
                            Component formattedKillerName = plugin.getChatManager()
                                    .getFormattedDisplayName(killer);
                            deathMessageTemplate = getRandomMessage(Arrays.asList(
                                    " was sent back to the lobby by %killer%.",
                                    " learned that %killer% is not their friend.",
                                    " was outplayed by %killer%."));
                            finalMessage = replaceKillerPlaceholder(formattedPlayerName, deathMessageTemplate, formattedKillerName);
                        } else {
                            deathMessageTemplate = getRandomMessage(Arrays.asList(
                                    " was slain by a " + damager.getType().name().toLowerCase(java.util.Locale.ROOT) + ".",
                                    " had a bone to pick with a " + damager.getType().name().toLowerCase(java.util.Locale.ROOT) + "."));
                            finalMessage = formatDeathMessage(formattedPlayerName, deathMessageTemplate);
                        }
                    } else {
                        finalMessage = formatDeathMessage(formattedPlayerName, "was killed by something.");
                    }
                    break;
                case ENTITY_EXPLOSION:
                case BLOCK_EXPLOSION:
                    Entity damager = null;
                    if (event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent edbe) {
                        damager = edbe.getDamager();
                    }
                    if (damager != null) {
                        if (damager instanceof Projectile projectile) {
                            if (projectile.getShooter() instanceof Entity shooterEntity) {
                                damager = shooterEntity;
                            }
                        }

                        Player killer = (damager instanceof Player p) ? p : null;
                        if (killer != null) {
                            Component formattedKillerName = plugin.getChatManager().getFormattedDisplayName(killer);
                            deathMessageTemplate = getRandomMessage(Arrays.asList(
                                    " was blown up by %killer%.",
                                    " learned that %killer% plays with explosives."));
                            finalMessage = replaceKillerPlaceholder(formattedPlayerName, deathMessageTemplate, formattedKillerName);
                        } else if (damager.getType() == EntityType.CREEPER) {
                            deathMessageTemplate = getRandomMessage(Arrays.asList(
                                    "got a hug from a Creeper.",
                                    "learned that some hugs are explosive."));
                            finalMessage = formatDeathMessage(formattedPlayerName, deathMessageTemplate);
                        } else if (damager.getType() == EntityType.TNT || damager.getType() == EntityType.TNT_MINECART) {
                            deathMessageTemplate = getRandomMessage(Arrays.asList(
                                    "played with TNT.",
                                    "was blown to pieces."));
                            finalMessage = formatDeathMessage(formattedPlayerName, deathMessageTemplate);
                        } else {
                            deathMessageTemplate = "was blown up by a " + damager.getType().name().toLowerCase(java.util.Locale.ROOT) + ".";
                            finalMessage = formatDeathMessage(formattedPlayerName, deathMessageTemplate);
                        }
                    } else {
                        deathMessageTemplate = getRandomMessage(Arrays.asList(
                                "should not have slept in the Nether.",
                                "'s bed went boom."));
                        finalMessage = formatDeathMessage(formattedPlayerName, deathMessageTemplate);
                    }
                    break;
                case FALL:
                    deathMessageTemplate = getRandomMessage(Arrays.asList(
                            "thought they were a bird.",
                            "forgot to deploy their parachute.",
                            "tested gravity. It still works."));
                    finalMessage = formatDeathMessage(formattedPlayerName, deathMessageTemplate);
                    break;
                case LAVA:
                    deathMessageTemplate = getRandomMessage(Arrays.asList(
                            "tried to swim in the forbidden soup.",
                            "is now one with the magma."));
                    finalMessage = formatDeathMessage(formattedPlayerName, deathMessageTemplate);
                    break;
                case DROWNING:
                    deathMessageTemplate = getRandomMessage(Arrays.asList(
                            "forgot how to breathe.",
                            "is sleeping with the fishes."));
                    finalMessage = formatDeathMessage(formattedPlayerName, deathMessageTemplate);
                    break;
                case VOID:
                    deathMessageTemplate = getRandomMessage(Arrays.asList(
                            "fell out of the world.",
                            "has been deleted from the simulation."));
                    finalMessage = formatDeathMessage(formattedPlayerName, deathMessageTemplate);
                    break;
                case FIRE:
                case FIRE_TICK:
                    deathMessageTemplate = getRandomMessage(Arrays.asList(
                            "is extra crispy now.",
                            "forgot to stop, drop, and roll."));
                    finalMessage = formatDeathMessage(formattedPlayerName, deathMessageTemplate);
                    break;
                default:
                    deathMessageTemplate = getRandomMessage(Arrays.asList(
                            "died in a mysterious way.",
                            "met their end."));
                    finalMessage = formatDeathMessage(formattedPlayerName, deathMessageTemplate);
                    break;
            }
            event.deathMessage(finalMessage);
        }
    }

    private Component replaceKillerPlaceholder(Component formattedPlayerName, String template, Component formattedKillerName) {
        if (template.contains("%killer%")) {
            String[] parts = template.split("%killer%", -1);
            String prefix = parts[0];
            if (!prefix.startsWith("'") && !prefix.startsWith(" ")) {
                prefix = " " + prefix;
            }
            String suffix = parts.length > 1 ? parts[1] : "";
            Component result = formattedPlayerName;
            if (!prefix.isEmpty()) {
                result = result.append(Component.text(prefix, NamedTextColor.RED));
            }
            result = result.append(formattedKillerName);
            if (!suffix.isEmpty()) {
                result = result.append(Component.text(suffix, NamedTextColor.RED));
            }
            return result;
        } else {
            String text = (template.startsWith("'") || template.startsWith(" ")) ? template : " " + template;
            return formattedPlayerName
                    .append(Component.text(text, NamedTextColor.RED))
                    .append(formattedKillerName);
        }
    }

    private Component formatDeathMessage(Component formattedPlayerName, String template) {
        if (template.startsWith("'")) {
            return formattedPlayerName.append(Component.text(template, NamedTextColor.RED));
        }
        String text = template.startsWith(" ") ? template : " " + template;
        return formattedPlayerName.append(Component.text(text, NamedTextColor.RED));
    }

    private String getRandomMessage(List<String> messages) {
        return messages.get(random.nextInt(messages.size()));
    }
}

