package com.mystic.recipefixer.mixins;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(RecipeManager.class)
public class MixinRecipeManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Inject(method = "apply", at = @At("RETURN"))
    private void onApply(Map<ResourceLocation, Recipe<?>> map, CallbackInfo ci) {
        int before = map.size();

        map.entrySet().removeIf(entry -> {
            Recipe<?> recipe = entry.getValue();
            if (recipe == null) return true;

            // Null or empty result
            ItemStack result = recipe.getResultItem(null);
            if (result == null || result.isEmpty()) {
                LOGGER.warn("[ItemStackFix] Removing recipe '{}' due to empty or null result", entry.getKey());
                return true;
            }

            // Empty ingredients
            List<Ingredient> ingredients = recipe.getIngredients();
            for (Ingredient ingredient : ingredients) {
                if (ingredient == null || ingredient.isEmpty()) {
                    LOGGER.warn("[ItemStackFix] Removing recipe '{}' due to empty or null ingredient", entry.getKey());
                    return true;
                }
            }

            return false;
        });

        int after = map.size();
        LOGGER.info("[ItemStackFix] Removed {} broken recipes during load", before - after);
    }
}
