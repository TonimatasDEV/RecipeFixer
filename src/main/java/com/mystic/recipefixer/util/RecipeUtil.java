package com.mystic.recipefixer.util;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public class RecipeUtil {
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
