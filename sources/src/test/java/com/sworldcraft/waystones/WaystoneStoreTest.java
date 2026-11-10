package com.sworldcraft.waystones;

import java.nio.file.*;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class WaystoneStoreTest {
    @TempDir Path world;
    @Test void discoveriesRemainPersonalAcrossRestartAndRename() {
        UUID a=UUID.randomUUID(), b=UUID.randomUUID();
        var store=new WaystoneStore(world);
        var maison=store.place("minecraft:overworld",new BlockPos(0,64,0),a);
        var mine=store.place("minecraft:overworld",new BlockPos(100,30,200),a);
        store.rename(maison,"Maison");store.rename(mine,"Mine");
        store.discover(a,maison);store.discover(a,mine);store.discover(b,maison);
        assertFalse(store.knows(b,mine.id));
        var restarted=new WaystoneStore(world);
        assertEquals(2,restarted.known(a).size());assertEquals(1,restarted.known(b).size());
        assertEquals("Maison",restarted.known(b).getFirst().name);
        restarted.rename(restarted.byId(maison.id),"Village");
        assertEquals("Village",new WaystoneStore(world).known(b).getFirst().name);
    }
    @Test void replacingAtSamePositionRequiresDiscoveryAgain() {
        UUID a=UUID.randomUUID();var store=new WaystoneStore(world);var pos=new BlockPos(4,60,4);
        var old=store.place("minecraft:overworld",pos,a);store.discover(a,old);
        var fresh=store.place("minecraft:overworld",pos,a);
        assertNotEquals(old.id,fresh.id);assertNull(store.byId(old.id));
        assertFalse(store.knows(a,fresh.id));assertTrue(new WaystoneStore(world).known(a).isEmpty());
    }
    @Test void dimensionsDoNotCollideAndDestroyedStonesAreForgotten() {
        UUID a=UUID.randomUUID();var store=new WaystoneStore(world);var pos=new BlockPos(4,60,4);
        var one=store.place("minecraft:overworld",pos,a);var two=store.place("minecraft:the_nether",pos,a);
        store.discover(a,one);store.discover(a,two);store.remove(one.id);
        var restarted=new WaystoneStore(world);
        assertEquals(two.id,restarted.known(a).getFirst().id);assertNull(restarted.byId(one.id));
    }
    @Test void corruptSaveIsNeverSilentlyOverwritten() throws Exception {
        Path file=world.resolve("data/sworldwaystones.json");Files.createDirectories(file.getParent());Files.writeString(file,"{broken");
        assertThrows(IllegalStateException.class,()->new WaystoneStore(world));assertEquals("{broken",Files.readString(file));
    }
    @Test void namesAreBoundedAndCannotInjectFormatting() {
        assertEquals("Maison",WaystoneStore.cleanName("  Mai\nson§  "));
        assertEquals(32,WaystoneStore.cleanName("x".repeat(100)).length());
        assertTrue(WaystoneStore.cleanName("\n\t  ").isEmpty());
    }
}
