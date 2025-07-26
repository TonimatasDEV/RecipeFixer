package com.mystic.itemstackemptyfix;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public class RecipePatcher {
    public static boolean isBroken(ItemStack result, Iterable<Ingredient> ingredients) {
        if (result == null || result.isEmpty()) {
            return true;
        }
        for (Ingredient ingredient : ingredients) {
            if (ingredient == null) return true;
            for (ItemStack stack : ingredient.getItems()) {
                if (stack == null || stack.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }
}
