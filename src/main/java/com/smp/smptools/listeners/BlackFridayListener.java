package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import com.smp.smptools.managers.BlackFridayManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BlackFridayListener implements Listener {

    private final BlackFridayManager manager;
    private final Map<UUID, List<MerchantRecipe>> originalRecipes = new ConcurrentHashMap<>();
    private final Map<UUID, java.util.Set<UUID>> activeViewers = new ConcurrentHashMap<>();

    public BlackFridayListener(SMPTools plugin, BlackFridayManager manager) {
        this.manager = manager;
    }

    public void restoreAllActive() {
        for (Map.Entry<UUID, List<MerchantRecipe>> entry : new java.util.HashMap<>(originalRecipes).entrySet()) {
            org.bukkit.entity.Entity entity = Bukkit.getEntity(entry.getKey());
            if (entity instanceof Merchant merchant) {
                restoreOriginalRecipes(merchant, entry.getValue());
            }
        }
        originalRecipes.clear();
        activeViewers.clear();
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        Inventory inventory = event.getInventory();
        if (inventory.getType() != InventoryType.MERCHANT) {
            return;
        }

        if (!(inventory.getHolder() instanceof AbstractVillager villager)) {
            return;
        }

        Merchant merchant = (Merchant) villager;
        UUID villagerUUID = villager.getUniqueId();
        UUID playerUUID = player.getUniqueId();

        if (!manager.isEnabled() || event.isCancelled()) {
            List<MerchantRecipe> original = originalRecipes.remove(villagerUUID);
            if (original != null) {
                restoreOriginalRecipes(merchant, original);
            }
            return;
        }

        java.util.Set<UUID> viewers = activeViewers.computeIfAbsent(villagerUUID, k -> ConcurrentHashMap.newKeySet());
        boolean isFirstViewer = viewers.isEmpty();
        viewers.add(playerUUID);

        if (isFirstViewer) {
            List<MerchantRecipe> original = new ArrayList<>(merchant.getRecipes());
            originalRecipes.put(villagerUUID, original);
            applyDiscount(merchant, original);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof AbstractVillager villager)) {
            return;
        }
        Merchant merchant = villager;
        UUID villagerUUID = villager.getUniqueId();
        UUID playerUUID = event.getPlayer().getUniqueId();

        java.util.Set<UUID> viewers = activeViewers.get(villagerUUID);
        if (viewers != null) {
            viewers.remove(playerUUID);
            if (viewers.isEmpty()) {
                activeViewers.remove(villagerUUID);
                List<MerchantRecipe> original = originalRecipes.remove(villagerUUID);
                if (original != null) {
                    restoreOriginalRecipes(merchant, original);
                }
            }
        }
    }

    private void restoreOriginalRecipes(Merchant merchant, List<MerchantRecipe> original) {
        List<MerchantRecipe> currentLive = merchant.getRecipes();
        List<MerchantRecipe> restored = new ArrayList<>();

        for (int i = 0; i < original.size(); i++) {
            MerchantRecipe base = original.get(i);
            MerchantRecipe restoredRecipe = new MerchantRecipe(base.getResult(), base.getMaxUses());
            restoredRecipe.setExperienceReward(base.hasExperienceReward());
            restoredRecipe.setVillagerExperience(base.getVillagerExperience());
            restoredRecipe.setPriceMultiplier(base.getPriceMultiplier());
            for (ItemStack ingredient : base.getIngredients()) {
                restoredRecipe.addIngredient(ingredient.clone());
            }

            if (i < currentLive.size()) {
                MerchantRecipe live = currentLive.get(i);
                restoredRecipe.setUses(live.getUses());
                restoredRecipe.setDemand(live.getDemand());
                restoredRecipe.setSpecialPrice(live.getSpecialPrice());
            } else {
                restoredRecipe.setUses(base.getUses());
                restoredRecipe.setDemand(base.getDemand());
                restoredRecipe.setSpecialPrice(base.getSpecialPrice());
            }

            restored.add(restoredRecipe);
        }

        merchant.setRecipes(restored);
    }

    private void applyDiscount(Merchant merchant, List<MerchantRecipe> baseRecipes) {
        List<MerchantRecipe> discountedRecipes = new ArrayList<>();
        int discountPercent = manager.getDiscountPercentage();

        for (MerchantRecipe recipe : baseRecipes) {
            List<ItemStack> ingredients = recipe.getIngredients();

            if (ingredients.isEmpty()) {
                discountedRecipes.add(recipe);
                continue;
            }

            ItemStack result = recipe.getResult().clone();
            ItemStack ingredient1 = ingredients.get(0).clone();
            int newAmount1 = Math.max(1, ingredient1.getAmount() * (100 - discountPercent) / 100);
            ingredient1.setAmount(newAmount1);

            MerchantRecipe newRecipe = new MerchantRecipe(result, recipe.getMaxUses());
            newRecipe.setUses(recipe.getUses());
            newRecipe.setDemand(recipe.getDemand());
            newRecipe.setSpecialPrice(recipe.getSpecialPrice());
            newRecipe.setExperienceReward(recipe.hasExperienceReward());
            newRecipe.setVillagerExperience(recipe.getVillagerExperience());
            newRecipe.setPriceMultiplier(recipe.getPriceMultiplier());
            newRecipe.addIngredient(ingredient1);

            if (ingredients.size() > 1) {
                ItemStack ingredient2 = ingredients.get(1).clone();
                int newAmount2 = Math.max(1, ingredient2.getAmount() * (100 - discountPercent) / 100);
                ingredient2.setAmount(newAmount2);
                newRecipe.addIngredient(ingredient2);
            }

            discountedRecipes.add(newRecipe);
        }

        merchant.setRecipes(discountedRecipes);
    }
}
