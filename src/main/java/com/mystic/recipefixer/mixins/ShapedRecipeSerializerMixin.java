package com.mystic.recipefixer.mixins;

import com.mojang.logging.LogUtils;
import com.mystic.recipefixer.util.RecipeUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.item.crafting.ShapedRecipe$Serializer")
public class ShapedRecipeSerializerMixin {
    @Inject(method = "toNetwork", at = @At("HEAD"), cancellable = true)
    private static void toNetworkPatch(RegistryFriendlyByteBuf buf, ShapedRecipe recipe, CallbackInfo ci) {
        if (RecipeUtil.isBroken(recipe.getResultItem(null), recipe.getIngredients())) {
            LogUtils.getLogger().error("[Mixin] BLOCKED broken ShapedRecipe: {}", recipe.getGroup());
            ci.cancel();
        }
    }

    @Inject(method = "fromNetwork", at = @At("HEAD"), cancellable = true)
    private static void fromNetworkPatch(RegistryFriendlyByteBuf buf, CallbackInfoReturnable<ShapedRecipe> cir) {
        try {
            String group = buf.readUtf();

            // Safe decode CraftingBookCategory
            int categoryOrdinal = buf.readVarInt();
            CraftingBookCategory[] categories = CraftingBookCategory.values();
            if (categoryOrdinal < 0 || categoryOrdinal >= categories.length) {
                LogUtils.getLogger().error("[Mixin] Invalid CraftingBookCategory index in shaped recipe '{}': {}", group, categoryOrdinal);
                cir.setReturnValue(null);
                return;
            }
            CraftingBookCategory category = categories[categoryOrdinal];

            // Safely decode pattern
            ShapedRecipePattern pattern;
            try {
                pattern = ShapedRecipePattern.STREAM_CODEC.decode(buf);
            } catch (Exception patternEx) {
                LogUtils.getLogger().error("[Mixin] Failed to decode ShapedRecipePattern in '{}': {}", group, patternEx.toString());
                cir.setReturnValue(null);
                return;
            }

            ItemStack result = ItemStack.STREAM_CODEC.decode(buf);

            if (RecipeUtil.isBroken(result, pattern.ingredients())) {
                LogUtils.getLogger().error("[Mixin] BLOCKED broken ShapedRecipe: {}", group);
                cir.setReturnValue(null);
                return;
            }

            cir.setReturnValue(new ShapedRecipe(group, category, pattern, result));
        } catch (Exception e) {
            LogUtils.getLogger().error("[Mixin] Exception decoding shaped recipe '{}': {}", e.getClass().getSimpleName(), e.getMessage());
            cir.setReturnValue(null);
        }
    }
}
