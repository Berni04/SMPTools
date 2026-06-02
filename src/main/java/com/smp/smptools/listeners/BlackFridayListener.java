package com.smp.smptools.listeners;

import com.smp.smptools.SMPTools;
import com.smp.smptools.managers.BlackFridayManager;
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

    public BlackFridayListener(SMPTools plugin, BlackFridayManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!manager.isEnabled()) {
            return;
        }

        if (!(event.getPlayer() instanceof Player)) {
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
        if (!originalRecipes.containsKey(villagerUUID)) {
            originalRecipes.put(villagerUUID, new ArrayList<>(merchant.getRecipes()));
        }

        applyDiscount(merchant);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!manager.isEnabled()) {
            return;
        }
        if (!(event.getInventory().getHolder() instanceof AbstractVillager villager)) {
            return;
        }
        // AbstractVillager implements Merchant
        Merchant merchant = villager;

        List<MerchantRecipe> original = originalRecipes.get(villager.getUniqueId());
        if (original != null) {
            merchant.setRecipes(original);
        }
    }

    private void applyDiscount(Merchant merchant) {
        List<MerchantRecipe> discountedRecipes = new ArrayList<>();
        int discountPercent = manager.getDiscountPercentage();

        for (MerchantRecipe recipe : merchant.getRecipes()) {
            ItemStack result = recipe.getResult().clone();
            List<ItemStack> ingredients = recipe.getIngredients();

            ItemStack ingredient1 = ingredients.get(0).clone();
            int newAmount1 = Math.max(1, ingredient1.getAmount() * (100 - discountPercent) / 100);
            ingredient1.setAmount(newAmount1);

            MerchantRecipe newRecipe = new MerchantRecipe(result, recipe.getMaxUses());
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
