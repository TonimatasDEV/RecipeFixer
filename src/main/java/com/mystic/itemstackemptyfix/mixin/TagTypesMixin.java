package com.mystic.itemstackemptyfix.mixin;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.TagType;
import net.minecraft.nbt.TagTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TagTypes.class)
public class TagTypesMixin {
    /**
     * Redirects {@link TagType#createInvalid(int)} to gracefully handle invalid tag IDs without crashing.
     * Returns {@link EndTag#TYPE} for safety if the tag ID is invalid.
     * @author Mysticpasta1
     * @reason Stop breaking my modpack!
     */
    @Redirect(method = "getType", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/TagType;createInvalid(I)Lnet/minecraft/nbt/TagType;"))
    private static TagType<?> fixer$getType(int id) {
        LogUtils.getLogger().error("[TagTypesMixin] Warning: Invalid tag ID: {}  — returning EndTag.TYPE as fallback.", id);
        System.err.println("[TagTypesMixin] Warning: Invalid tag ID: " + id + " — returning EndTag.TYPE as fallback.");
        return EndTag.TYPE; // Safe no-op tag that won't crash
    }
}
