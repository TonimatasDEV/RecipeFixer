package com.mystic.itemstackemptyfix.mixin;

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
            System.out.println("[Mixin] BLOCKED broken ShapelessRecipe: " + recipe.getGroup());
            ci.cancel();
        }
    }

    @Inject(method = "fromNetwork", at = @At("HEAD"), cancellable = true)
    private static void fromNetworkPatch(RegistryFriendlyByteBuf buf, CallbackInfoReturnable<ShapelessRecipe> cir) {
        try {
            String group = buf.readUtf();
            CraftingBookCategory category = CraftingBookCategory.STREAM_CODEC.decode(buf);

            int ingredientCount = buf.readVarInt();
            NonNullList<Ingredient> ingredients = NonNullList.create();

            for (int i = 0; i < ingredientCount; ++i) {
                try {
                    Ingredient ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
                    ingredients.add(ingredient);
                } catch (Exception e) {
                    System.err.println("[Mixin] Replacing corrupt ingredient in '" + group + "': " + e);
                    ingredients.add(Ingredient.EMPTY);
                }
            }

            ItemStack result = ItemStack.STREAM_CODEC.decode(buf);

            if (result == null || result.isEmpty()) {
                System.err.println("[Mixin] Skipping shapeless recipe with empty result: " + group);
                cir.setReturnValue(null);
                return;
            }

            cir.setReturnValue(new ShapelessRecipe(group, category, result, ingredients));
        } catch (Exception ex) {
            System.err.println("[Mixin] Exception decoding ShapelessRecipe: " + ex);
            cir.setReturnValue(null);
        }
    }
}
