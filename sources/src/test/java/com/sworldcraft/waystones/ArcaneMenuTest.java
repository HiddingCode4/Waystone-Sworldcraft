package com.sworldcraft.waystones;

import java.util.*;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.RegistryAccess;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArcaneMenuTest {
    private final UUID owner=UUID.randomUUID();
    private WaystoneStore.Stone stone(String name,int i) {
        var s=new WaystoneStore.Stone();s.name=name;s.owner=owner.toString();s.id=UUID.randomUUID().toString();s.dimension="minecraft:overworld";s.x=i;s.y=64;s.z=4;return s;
    }
    @Test void searchCoversAllPagesAndPageIsClamped() {
        var source=stone("Maison",0);var known=new ArrayList<WaystoneStore.Stone>();
        for(int i=0;i<17;i++)known.add(stone("Village "+i,i));
        known.add(stone("Mine lointaine",20));
        var found=ArcaneMenu.snapshot(source,owner,known,"  MINE  ",999,false);
        assertEquals(0,found.page());assertEquals(1,found.pages());assertEquals(18,found.knownCount());
        assertEquals("Mine lointaine",found.destinations().getFirst().name());
        var last=ArcaneMenu.snapshot(source,owner,known,"",999,false);
        assertEquals(3,last.page());assertEquals(3,last.destinations().size());
        assertEquals(0,ArcaneMenu.snapshot(source,owner,known,"",-100,false).page());
    }
    @Test void visitorCannotEnterRenameMode() {
        var source=stone("Maison",0);
        var view=ArcaneMenu.snapshot(source,UUID.randomUUID(),List.of(),"",0,true);
        assertFalse(view.owner());assertFalse(view.naming());
        assertTrue(ArcaneMenu.snapshot(source,owner,List.of(),"",0,true).naming());
    }
    @Test void snapshotIsSmallAndDoesNotTransmitPrivateIdentifiers() {
        var source=stone("Maison",0);var dest=stone("Village",1);
        var snapshot=ArcaneMenu.snapshot(source,owner,List.of(dest),"",0,false);
        var buf=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        try {
            snapshot.write(buf);assertTrue(buf.readableBytes()<32600);
            var read=ArcaneMenu.Snapshot.read(buf);
            assertEquals("Village",read.destinations().getFirst().name());assertEquals("",read.destinations().getFirst().id());
            assertEquals(1,read.destinations().getFirst().x());assertEquals(0,buf.readableBytes());
        } finally {buf.release();}
    }
    @Test void emptySearchIsAValidSinglePage() {
        var data=ArcaneMenu.snapshot(stone("Maison",0),owner,List.of(stone("Mine",1)),"Introuvable",5,false);
        assertTrue(data.destinations().isEmpty());assertEquals(1,data.pages());assertEquals(0,data.page());
    }
}
