package com.mystic.recipefixer.mixins;

import com.mojang.logging.LogUtils;
import com.mystic.recipefixer.util.RecipeUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.item.crafting.ShapedRecipe$Serializer")
public class MixinShapedRecipeSerializer {
    @Unique
    private static final Logger itemstackemptyfix$LOGGER = LogUtils.getLogger();

    @Inject(method = "toNetwork", at = @At("HEAD"), cancellable = true)
    private static void toNetworkPatch(RegistryFriendlyByteBuf buf, ShapedRecipe recipe, CallbackInfo ci) {
        try {
            ItemStack result = recipe.getResultItem(null);
            if (result == null || result.isEmpty()) {
                itemstackemptyfix$LOGGER.warn("[Mixin] Skipping shaped recipe '{}' due to empty result in toNetwork.", recipe.getGroup());
                ci.cancel();
                return;
            }

            boolean hasValidIngredient = false;
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient != null && !ingredient.isEmpty()) {
                    hasValidIngredient = true;
                    break;
                }
            }

            if (!hasValidIngredient) {
                itemstackemptyfix$LOGGER.warn("[Mixin] Skipping shaped recipe '{}' due to no valid ingredients in toNetwork.", recipe.getGroup());
                ci.cancel();
            }

        } catch (Exception e) {
            itemstackemptyfix$LOGGER.error("[Mixin] Error validating shaped recipe during toNetwork: {}", e.getMessage(), e);
            ci.cancel(); // fail-safe
        }
    }

    @Inject(method = "fromNetwork", at = @At("RETURN"), cancellable = true)
    private static void postDeserializePatch(RegistryFriendlyByteBuf buf, CallbackInfoReturnable<ShapedRecipe> cir) {
        ShapedRecipe recipe = cir.getReturnValue();
        if (recipe == null) return;

        try {
            ItemStack result = recipe.getResultItem(null);
            if (result == null || result.isEmpty()) {
                itemstackemptyfix$LOGGER.warn("[Mixin] Skipping shaped recipe after decode due to empty result: {}", recipe.getGroup());
                cir.setReturnValue(null);
                return;
            }

            boolean hasValidIngredient = false;
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient != null && !ingredient.isEmpty()) {
                    hasValidIngredient = true;
                    break;
                }
            }

            if (!hasValidIngredient) {
                itemstackemptyfix$LOGGER.warn("[Mixin] Skipping shaped recipe after decode due to no valid ingredients: {}", recipe.getGroup());
                cir.setReturnValue(null);
            }

        } catch (Exception e) {
            itemstackemptyfix$LOGGER.error("[Mixin] Exception while inspecting shaped recipe after decode: {}", e.getMessage(), e);
            cir.setReturnValue(null);
        }
    }
}
