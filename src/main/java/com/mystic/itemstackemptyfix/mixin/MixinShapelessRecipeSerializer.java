package com.mystic.itemstackemptyfix.mixin;

import com.mystic.itemstackemptyfix.RecipePatcher;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.item.crafting.ShapelessRecipe$Serializer")
public class MixinShapelessRecipeSerializer {
    @Inject(method = "toNetwork", at = @At("HEAD"), cancellable = true)
    private static void patch(RegistryFriendlyByteBuf buffer, ShapelessRecipe recipe, CallbackInfo ci) {
        if (RecipePatcher.isBroken(recipe.getResultItem(null), recipe.getIngredients())) {
            System.out.println("[Mixin] BLOCKED broken ShapelessRecipe: " + recipe.getGroup());
            ci.cancel();
        }
    }
}
