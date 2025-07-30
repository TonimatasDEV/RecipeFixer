package com.mystic.recipefixer.mixins;

import com.mojang.logging.LogUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.item.crafting.ShapelessRecipe$Serializer")
public class MixinShapelessRecipeSerializer {
    @Unique
    private static final Logger itemstackemptyfix$LOGGER = LogUtils.getLogger();

    @Inject(method = "toNetwork", at = @At("HEAD"), cancellable = true)
    private static void toNetworkPatch(RegistryFriendlyByteBuf buf, ShapelessRecipe recipe, CallbackInfo ci) {
        try {
            ItemStack result = recipe.getResultItem(null);
            if (result == null || result.isEmpty()) {
                itemstackemptyfix$LOGGER.warn("[Mixin] ShapelessRecipe '{}' skipped due to empty result in toNetwork.", recipe.getGroup());
                ci.cancel();
                return;
            }

            for (Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient == null || ingredient.isEmpty()) continue;

                for (ItemStack stack : ingredient.getItems()) {
                    if (stack == null || stack.isEmpty()) {
                        itemstackemptyfix$LOGGER.warn("[Mixin] ShapelessRecipe '{}' skipped due to invalid ingredient (ItemStack.EMPTY) in toNetwork.", recipe.getGroup());
                        ci.cancel();
                        return;
                    }
                }
            }

        } catch (Exception e) {
            itemstackemptyfix$LOGGER.error("[Mixin] Exception in ShapelessRecipe toNetworkPatch: {}", e.getMessage(), e);
            ci.cancel(); // fail-safe
        }
    }

    @Inject(method = "fromNetwork", at = @At("RETURN"), cancellable = true)
    private static void postDeserializePatch(RegistryFriendlyByteBuf buf, CallbackInfoReturnable<ShapelessRecipe> cir) {
        ShapelessRecipe recipe = cir.getReturnValue();
        if (recipe == null) return;

        try {
            ItemStack result = recipe.getResultItem(null);
            if (result == null || result.isEmpty()) {
                itemstackemptyfix$LOGGER.warn("[Mixin] ShapelessRecipe '{}' skipped after decode due to empty result.", recipe.getGroup());
                cir.setReturnValue(null);
                return;
            }

            for (Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient == null || ingredient.isEmpty()) continue;

                for (ItemStack stack : ingredient.getItems()) {
                    if (stack == null || stack.isEmpty()) {
                        itemstackemptyfix$LOGGER.warn("[Mixin] ShapelessRecipe '{}' skipped after decode due to empty ingredient ItemStack.", recipe.getGroup());
                        cir.setReturnValue(null);
                        return;
                    }
                }
            }

        } catch (Exception e) {
            itemstackemptyfix$LOGGER.error("[Mixin] Error inspecting shapeless recipe '{}': {}", recipe.getGroup(), e.getMessage(), e);
            cir.setReturnValue(null);
        }
    }
}
