package com.mystic.itemstackemptyfix.mixin;

import com.mojang.logging.LogUtils;
import com.mystic.itemstackemptyfix.RecipePatcher;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.item.crafting.ShapelessRecipe$Serializer")
public class MixinShapelessRecipeSerializer {
    @Inject(method = "toNetwork", at = @At("HEAD"), cancellable = true)
    private static void toNetworkPatch(RegistryFriendlyByteBuf buf, ShapelessRecipe recipe, CallbackInfo ci) {
        if (RecipePatcher.isBroken(recipe.getResultItem(null), recipe.getIngredients())) {
            LogUtils.getLogger().error("[Mixin] BLOCKED broken ShapelessRecipe: {}", recipe.getGroup());
            ci.cancel();
        }
    }

    @Inject(method = "fromNetwork", at = @At("HEAD"), cancellable = true)
    private static void fromNetworkPatch(RegistryFriendlyByteBuf buf, CallbackInfoReturnable<ShapelessRecipe> cir) {
        try {
            String group = buf.readUtf();

            CraftingBookCategory category;
            try {
                category = CraftingBookCategory.STREAM_CODEC.decode(buf);
            } catch (Exception e) {
                LogUtils.getLogger().error("[Mixin] Failed to decode CraftingBookCategory in '{}': {}", group, e.toString());
                cir.setReturnValue(null);
                return;
            }

            int ingredientCount = buf.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.create();

            for (int i = 0; i < ingredientCount; ++i) {
                try {
                    Ingredient ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
                    ingredients.add(ingredient);
                } catch (Exception e) {
                    LogUtils.getLogger().warn("[Mixin] Replacing corrupt ingredient at index {} in '{}': {}", i, group, e.toString());
                    ingredients.add(Ingredient.EMPTY);
                }
            }

            ItemStack result;
            try {
                result = ItemStack.STREAM_CODEC.decode(buf);
            } catch (Exception e) {
                LogUtils.getLogger().error("[Mixin] Failed to decode result ItemStack in '{}': {}", group, e.toString());
                cir.setReturnValue(null);
                return;
            }

            if (result == null || result.isEmpty()) {
                LogUtils.getLogger().error("[Mixin] Skipping shapeless recipe with empty result: {}", group);
                cir.setReturnValue(null);
                return;
            }

            cir.setReturnValue(new ShapelessRecipe(group, category, result, ingredients));
        } catch (Exception ex) {
            LogUtils.getLogger().error("[Mixin] Unexpected error decoding ShapelessRecipe: {}", ex.toString());
            cir.setReturnValue(null);
        }
    }
}
