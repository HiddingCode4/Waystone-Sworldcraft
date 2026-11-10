package com.sworldcraft.waystones;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(EphemeralTestServerProvider.class)
class RegistrationTest {
    @Test void recipeAndBlockLootLoadOnServer(MinecraftServer server) {
        assertTrue(server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE,
            Identifier.parse("sworldwaystones:waystone"))).isPresent(),"Crafting recipe must load");
        var table=server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
            Identifier.parse("sworldwaystones:blocks/waystone")));
        assertNotSame(LootTable.EMPTY,table,"Block loot table must load");
        assertSame(LootContextParamSets.BLOCK,table.getParamSet());
        assertSame(SworldWaystones.WAYSTONE.get(),SworldWaystones.WAYSTONE_ITEM.get().getBlock());
    }
}
