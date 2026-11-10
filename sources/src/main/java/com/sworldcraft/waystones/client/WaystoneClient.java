package com.sworldcraft.waystones.client;

import com.sworldcraft.waystones.SworldWaystones;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@Mod(value=SworldWaystones.ID,dist=Dist.CLIENT)
public final class WaystoneClient {
    public WaystoneClient(IEventBus bus) { bus.addListener(this::screens); }
    private void screens(RegisterMenuScreensEvent event) { event.register(SworldWaystones.ARCANE_MENU.get(),ArcaneScreen::new); }
}
