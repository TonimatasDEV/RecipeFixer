package com.mystic.itemstackemptyfix.mixin;

import com.mystic.itemstackemptyfix.RecipePatcher;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.item.crafting.ShapedRecipe$Serializer")
public class MixinShapedRecipeSerializer {
    @Inject(method = "toNetwork", at = @At("HEAD"), cancellable = true)
    private static void toNetworkPatch(RegistryFriendlyByteBuf buf, ShapedRecipe recipe, CallbackInfo ci) {
        if (RecipePatcher.isBroken(recipe.getResultItem(null), recipe.getIngredients())) {
            System.out.println("[Mixin] BLOCKED broken ShapedRecipe: " + recipe.getGroup());
            ci.cancel();
        }
    }

    @Inject(method = "fromNetwork", at = @At("HEAD"), cancellable = true)
    private static void fromNetworkPatch(RegistryFriendlyByteBuf buf, CallbackInfoReturnable<ShapedRecipe> cir) {
        try {
            // Read group and category first (match ShapedRecipe constructor)
            String group = buf.readUtf();
            CraftingBookCategory category = CraftingBookCategory.STREAM_CODEC.decode(buf);

            int width = buf.readVarInt();
            int height = buf.readVarInt();

            // Read ingredients count = width * height
            NonNullList<Ingredient> ingredients = NonNullList.withSize(width * height, Ingredient.EMPTY);
            for (int i = 0; i < width * height; ++i) {
                ingredients.set(i, Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
            }

            ItemStack result = ItemStack.STREAM_CODEC.decode(buf);

            if (RecipePatcher.isBroken(result, ingredients)) {
                System.out.println("[Mixin] BLOCKED broken ShapedRecipe: " + group);
                cir.setReturnValue(null);
            }

            // If all good, let vanilla deserialize normally (cancel injection, allow normal return)
        } catch (Exception e) {
            System.err.println("[Mixin] Exception reading ShapedRecipe: " + e);
            cir.setReturnValue(null);
        }
    }
}
