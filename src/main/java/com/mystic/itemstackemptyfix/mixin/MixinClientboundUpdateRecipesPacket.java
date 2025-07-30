package com.mystic.itemstackemptyfix.mixin;

import com.mojang.logging.LogUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(ClientboundUpdateRecipesPacket.class)
public abstract class MixinClientboundUpdateRecipesPacket {
    @Mutable
    @Shadow
    @Final
    public static StreamCodec<RegistryFriendlyByteBuf, ClientboundUpdateRecipesPacket> STREAM_CODEC;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void patchCodec(CallbackInfo ci) {
        final Logger LOGGER = LogUtils.getLogger();

        STREAM_CODEC = new StreamCodec<>() {
            @Override
            public ClientboundUpdateRecipesPacket decode(RegistryFriendlyByteBuf buf) {
                int count = buf.readVarInt();
                List<RecipeHolder<?>> recipes = new ArrayList<>();

                for (int i = 0; i < count; i++) {
                    buf.markReaderIndex(); // allow rollback

                    try {
                        RecipeHolder<?> holder = RecipeHolder.STREAM_CODEC.decode(buf);

                        // Validate the ID
                        ResourceLocation id = holder.id();
                        if (!isValidPath(id.getPath())) {
                            LOGGER.warn("[SafeRecipes] Skipping recipe with invalid path '{}'", id);
                            continue;
                        }

                        recipes.add(holder);
                    } catch (Exception e) {
                        LOGGER.error("[SafeRecipes] Skipping corrupt recipe at index {}: {}", i, e.toString());
                        buf.resetReaderIndex(); // prevent packet corruption
                        skipUnknownRecipe(buf);
                    }
                }

                return new ClientboundUpdateRecipesPacket(recipes);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, ClientboundUpdateRecipesPacket packet) {
                RecipeHolder.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, packet.getRecipes());
            }

            private boolean isValidPath(String path) {
                return path.matches("[a-z0-9_./-]+");
            }

            private void skipUnknownRecipe(RegistryFriendlyByteBuf buf) {
                try {
                    // Best-effort skip: recipe ID + maybe some payload
                    buf.readResourceLocation(); // attempt to consume the ID
                    buf.skipBytes(buf.readableBytes()); // skip rest
                } catch (Exception ignored) {}
            }
        };

        LOGGER.info("[SafeRecipes] Patched ClientboundUpdateRecipesPacket.STREAM_CODEC to safely skip broken recipes");
    }
}
