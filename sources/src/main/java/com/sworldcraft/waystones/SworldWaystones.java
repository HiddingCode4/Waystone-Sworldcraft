package com.sworldcraft.waystones;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

@Mod(SworldWaystones.ID)
public final class SworldWaystones {
    public static final String ID = "sworldwaystones";
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ID);
    public static final DeferredBlock<WaystoneBlock> WAYSTONE = BLOCKS.registerBlock("waystone", WaystoneBlock::new,
        properties -> properties.mapColor(MapColor.COLOR_PURPLE).strength(3.5F, 1200F).noOcclusion()
            .pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE).lightLevel(state -> 10));
    public static final DeferredItem<BlockItem> WAYSTONE_ITEM = ITEMS.registerSimpleBlockItem("waystone", WAYSTONE);

    public static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS = DeferredRegister.create(net.minecraft.core.registries.Registries.MENU, ID);
    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.inventory.MenuType<?>, net.minecraft.world.inventory.MenuType<ArcaneMenu>> ARCANE_MENU =
        MENUS.register("arcane_waystone", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(ArcaneMenu::new));

    public SworldWaystones(IEventBus bus) {
        MENUS.register(bus);
        bus.addListener(ArcaneAction::register);
        BLOCKS.register(bus);
        ITEMS.register(bus);
        bus.addListener(this::creative);
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> WaystoneStore.release(event.getServer()));
    }
    private void creative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) event.accept(WAYSTONE_ITEM);
    }
}
