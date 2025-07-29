package com.mystic.itemstackemptyfix.mixin;

import net.minecraft.nbt.TagType;
import net.minecraft.nbt.TagTypes;
import net.minecraft.nbt.EndTag;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(TagTypes.class)
public class TagTypesMixin {

    @Shadow @Final private static TagType<?>[] TYPES;

    /**
     * Overwrites getType to gracefully handle invalid tag IDs without crashing.
     * Returns EndTag.TYPE for safety if the tag ID is invalid.
     * @author Mysticpasta1
     * @reason stop breaking my modpack!
     */
    @Overwrite
    public static TagType<?> getType(int id) {
        if (id >= 0 && id < TYPES.length) {
            return TYPES[id];
        }

        System.err.println("[TagTypesMixin] Warning: Invalid tag ID: " + id + " — returning EndTag.TYPE as fallback.");
        return EndTag.TYPE; // Safe no-op tag that won't crash
    }
}
