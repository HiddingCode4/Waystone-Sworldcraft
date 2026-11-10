package com.sworldcraft.waystones;

import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;

/** Vanilla menu protocol: every click is handled on the server; no client-trusted coordinates. */
public final class WaystoneMenus {
    private WaystoneMenus() {}
    private static WaystoneStore store(ServerPlayer p) { return WaystoneStore.get(p.level().getServer()); }
    static boolean valid(ServerPlayer p,WaystoneStore.Stone s) {
        return p.isAlive() && !p.isSpectator() && store(p).byId(s.id)==s
            && p.level().dimension().equals(s.dimensionKey()) && p.distanceToSqr(Vec3.atCenterOf(s.pos()))<=64
            && p.level().getBlockState(s.pos()).is(SworldWaystones.WAYSTONE.get());
    }
    public static void open(ServerPlayer p,WaystoneStore.Stone source,int page) {
        openPage(p,source,"",page,false);
    }
    public static void openRename(ServerPlayer p,WaystoneStore.Stone source,boolean first) {
        if(source.owner.equals(p.getUUID().toString())) openPage(p,source,"",0,true);
    }
    static void openPage(ServerPlayer p,WaystoneStore.Stone source,String query,int page,boolean naming) {
        if(!valid(p,source)) return;
        var registry=store(p);
        for(var stone: List.copyOf(registry.known(p.getUUID()))) {
            var level=p.level().getServer().getLevel(stone.dimensionKey());
            if(level!=null && level.hasChunkAt(stone.pos()) && !level.getBlockState(stone.pos()).is(SworldWaystones.WAYSTONE.get())) registry.remove(stone.id);
        }
        var known=registry.known(p.getUUID()).stream().filter(s->!s.id.equals(source.id)).toList();
        var snapshot=ArcaneMenu.snapshot(source,p.getUUID(),known,query,page,naming);
        p.openMenu(new SimpleMenuProvider((id,inv,who)->new ArcaneMenu(id,p,source,snapshot),
            Component.literal("Sanctuaire des passages")),snapshot::write);
    }
    static void rename(ServerPlayer p,WaystoneStore.Stone source,String name) {
        if(!valid(p,source) || !source.owner.equals(p.getUUID().toString())) return;
        String clean=WaystoneStore.cleanName(name);
        if(clean.isBlank()) return;
        store(p).rename(source,clean);
        message(p,"Waystone nommée : "+clean);
        open(p,source,0);
    }
    static void teleport(ServerPlayer p,WaystoneStore.Stone source,String id) {
        var registry=store(p);
        if(!valid(p,source) || !registry.knows(p.getUUID(),id) || id.equals(source.id)) return;
        var dest=registry.byId(id); if(dest==null)return;
        if(p.isPassenger() || p.isSleeping() || p.isFallFlying()) {
            message(p,"Descends de ta monture et termine ton déplacement avant de te téléporter.");return;
        }
        ServerLevel level=p.level().getServer().getLevel(dest.dimensionKey());
        if(level==null) { message(p,"Cette dimension est indisponible.");return; }
        level.getChunk(dest.pos());
        var state=level.getBlockState(dest.pos());
        if(!state.is(SworldWaystones.WAYSTONE.get()) || state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF)!=DoubleBlockHalf.LOWER) {
            registry.remove(id); message(p,"Cette Waystone a été détruite.");open(p,source,0);return;
        }
        Vec3 arrival=findArrival(level,p,dest.pos());
        if(arrival==null) { message(p,"Aucun emplacement sûr autour de cette Waystone.");return; }
        p.closeContainer();
        if(!p.teleportTo(level,arrival.x,arrival.y,arrival.z,Set.of(),p.getYRot(),p.getXRot(),true)) {
            message(p,"Téléportation refusée par le serveur."); return;
        }
        p.fallDistance=0;
        p.setDeltaMovement(Vec3.ZERO);
        message(p,"Téléportation vers "+dest.name);
    }
    private static void message(ServerPlayer p,String text) { p.sendOverlayMessage(Component.literal(text)); }
    private static Vec3 findArrival(ServerLevel level,ServerPlayer p,BlockPos base) {
        for(int radius=1;radius<=3;radius++) for(int dy : new int[]{0,1,-1,2})
            for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++) {
                if(Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
                BlockPos feet=base.offset(dx,dy,dz);
                if(feet.getY()<=level.getMinY() || feet.getY()+2>level.getMaxY() || !level.getWorldBorder().isWithinBounds(feet))continue;
                if(!level.getBlockState(feet.below()).isFaceSturdy(level,feet.below(),net.minecraft.core.Direction.UP))continue;
                boolean hazard=false;
                for(BlockPos q : new BlockPos[]{feet.below(),feet,feet.above()}) {
                    var s=level.getBlockState(q);
                    if(!s.getFluidState().isEmpty() || s.is(Blocks.MAGMA_BLOCK) || s.is(Blocks.CACTUS) || s.is(Blocks.FIRE)
                        || s.is(Blocks.SOUL_FIRE) || s.is(Blocks.CAMPFIRE) || s.is(Blocks.SOUL_CAMPFIRE)
                        || s.is(Blocks.WITHER_ROSE) || s.is(Blocks.SWEET_BERRY_BUSH) || s.is(Blocks.POWDER_SNOW))hazard=true;
                }
                Vec3 v=Vec3.atBottomCenterOf(feet);
                AABB bounds=p.getDimensions(net.minecraft.world.entity.Pose.STANDING).makeBoundingBox(v);
                if(!hazard && level.noCollision(p,bounds) && level.getWorldBorder().isWithinBounds(bounds))return v;
            }
        return null;
    }
}
