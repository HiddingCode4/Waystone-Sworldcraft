package com.sworldcraft.waystones;

import java.util.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Slot-free menu. The server owns the destination IDs; the client sends only an index. */
public final class ArcaneMenu extends AbstractContainerMenu {
    public static final int PAGE_SIZE=5;
    public record Destination(String id,String name,String dimension,int x,int y,int z) {}
    public record Snapshot(String sourceName,boolean owner,boolean naming,String query,int page,int pages,int knownCount,List<Destination> destinations) {
        public Snapshot { destinations=List.copyOf(destinations); }
        public void write(RegistryFriendlyByteBuf buf) {
            buf.writeUtf(sourceName,32);buf.writeBoolean(owner);buf.writeBoolean(naming);buf.writeUtf(query,64);
            buf.writeVarInt(page);buf.writeVarInt(pages);buf.writeVarInt(knownCount);buf.writeVarInt(destinations.size());
            for(var d:destinations) {
                // No UUID is needed on the client, nor names of undiscovered stones.
                buf.writeUtf(d.name(),32);buf.writeUtf(d.dimension(),256);
                buf.writeInt(d.x());buf.writeInt(d.y());buf.writeInt(d.z());
            }
        }
        public static Snapshot read(RegistryFriendlyByteBuf buf) {
            String source=buf.readUtf(32);boolean owner=buf.readBoolean(),naming=buf.readBoolean();String query=buf.readUtf(64);
            int page=buf.readVarInt(),pages=buf.readVarInt(),total=buf.readVarInt(),count=buf.readVarInt();
            if(count<0 || count>PAGE_SIZE || page<0 || pages<1 || page>=pages || total<0) throw new IllegalArgumentException("Invalid waystone menu");
            List<Destination> entries=new ArrayList<>();
            for(int i=0;i<count;i++) entries.add(new Destination("",buf.readUtf(32),buf.readUtf(256),buf.readInt(),buf.readInt(),buf.readInt()));
            return new Snapshot(source,owner,naming,query,page,pages,total,entries);
        }
    }
    static Snapshot snapshot(WaystoneStore.Stone source,UUID viewer,List<WaystoneStore.Stone> known,String query,int page,boolean naming) {
        query=query.strip();if(query.length()>64)query=query.substring(0,64);
        String needle=query.toLowerCase(Locale.ROOT);
        var filtered=known.stream().filter(s->s.name.toLowerCase(Locale.ROOT).contains(needle)).toList();
        int pages=Math.max(1,(filtered.size()+PAGE_SIZE-1)/PAGE_SIZE);page=Math.clamp(page,0,pages-1);
        int start=page*PAGE_SIZE;
        var entries=filtered.subList(start,Math.min(start+PAGE_SIZE,filtered.size())).stream()
            .map(s->new Destination(s.id,s.name,s.dimension,s.x,s.y,s.z)).toList();
        boolean owner=source.owner.equals(viewer.toString());
        return new Snapshot(source.name,owner,naming&&owner,query,page,pages,known.size(),entries);
    }
    public final Snapshot snapshot;
    private final ServerPlayer viewer;
    private final WaystoneStore.Stone source;
    public ArcaneMenu(int id,Inventory inv,RegistryFriendlyByteBuf buf) {
        super(SworldWaystones.ARCANE_MENU.get(),id);snapshot=Snapshot.read(buf);viewer=null;source=null;
    }
    ArcaneMenu(int id,ServerPlayer viewer,WaystoneStore.Stone source,Snapshot snapshot) {
        super(SworldWaystones.ARCANE_MENU.get(),id);this.viewer=viewer;this.source=source;this.snapshot=snapshot;
    }
    @Override public boolean stillValid(Player p) { return viewer==null || (p==viewer && WaystoneMenus.valid(viewer,source)); }
    @Override public ItemStack quickMoveStack(Player p,int index) { return ItemStack.EMPTY; }
    @Override public boolean clickMenuButton(Player p,int button) {
        if(viewer==null || p!=viewer || !stillValid(p))return false;
        if(button>=0 && button<snapshot.destinations().size() && !snapshot.naming()) {
            WaystoneMenus.teleport(viewer,source,snapshot.destinations().get(button).id());return true;
        }
        if(button==-1 && snapshot.owner()) { WaystoneMenus.openRename(viewer,source,false);return true; }
        if(button==-2) { WaystoneMenus.openPage(viewer,source,snapshot.query(),snapshot.page(),false);return true; }
        return false;
    }
    void action(ServerPlayer p,int action,String text,int page) {
        if(p!=viewer || !stillValid(p))return;
        if(action==0)WaystoneMenus.openPage(viewer,source,text,page,false);
        if(action==1 && snapshot.owner() && snapshot.naming())WaystoneMenus.rename(viewer,source,text);
    }
}
