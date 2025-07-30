package com.mystic.recipefixer;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(RecipeFixer.MODID)
public class RecipeFixer {
    public static final String MODID = "recipefixer";

    public RecipeFixer(IEventBus modEventBus, ModContainer modContainer) {
        LogUtils.getLogger().info("Recipe Fixer has been started correctly.");
    }
}
