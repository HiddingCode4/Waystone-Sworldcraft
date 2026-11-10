package com.sworldcraft.waystones;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

/** Server-thread only. IDs distinguish a replaced stone from a previously discovered one. */
public final class WaystoneStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<MinecraftServer, WaystoneStore> STORES = new IdentityHashMap<>();
    public static final class Stone {
        public String id, dimension, owner, name;
        public int x, y, z;
        public BlockPos pos() { return new BlockPos(x,y,z); }
        public ResourceKey<Level> dimensionKey() {
            return ResourceKey.create(Registries.DIMENSION, Identifier.parse(dimension));
        }
    }
    static final class Data {
        int format = 1;
        Map<String, Stone> stones = new LinkedHashMap<>();
        Map<String, Set<String>> discoveries = new HashMap<>();
    }
    private final Path file;
    private Data data;
    private WaystoneStore(MinecraftServer server) { this(server.getWorldPath(LevelResource.ROOT)); }
    WaystoneStore(Path worldRoot) {
        file = worldRoot.resolve("data/sworldwaystones.json");
        try {
            data = Files.exists(file) ? GSON.fromJson(Files.readString(file), Data.class) : new Data();
            if (data == null || data.format != 1 || data.stones == null || data.discoveries == null)
                throw new IOException("Format de sauvegarde invalide");
            for (var e : data.stones.entrySet()) {
                Stone s = e.getValue();
                if (s == null || !e.getKey().equals(s.id) || s.name == null) throw new IOException("Waystone invalide");
                UUID.fromString(s.id); UUID.fromString(s.owner); s.dimensionKey();
            }
        } catch (Exception ex) {
            // Never silently overwrite a corrupt or unreadable world registry.
            throw new IllegalStateException("Impossible de lire " + file + ". Conserver ce fichier pour réparation.", ex);
        }
    }
    public static WaystoneStore get(MinecraftServer server) { return STORES.computeIfAbsent(server, WaystoneStore::new); }
    public static void release(MinecraftServer server) { STORES.remove(server); }
    public Stone at(ServerLevel level, BlockPos pos) {
        return at(level.dimension().identifier().toString(),pos);
    }
    Stone at(String dim,BlockPos pos) {
        return data.stones.values().stream().filter(s -> s.dimension.equals(dim) && s.pos().equals(pos)).findFirst().orElse(null);
    }
    public Stone byId(String id) { return data.stones.get(id); }
    public Stone place(ServerLevel level, BlockPos pos, UUID owner) {
        return place(level.dimension().identifier().toString(),pos,owner);
    }
    Stone place(String dimension,BlockPos pos,UUID owner) {
        Stone previous = at(dimension,pos);
        if (previous != null) remove(previous.id);
        Stone s = new Stone(); s.id=UUID.randomUUID().toString(); s.dimension=dimension;
        s.owner=owner.toString(); s.name="Nouvelle Waystone"; s.x=pos.getX(); s.y=pos.getY(); s.z=pos.getZ();
        data.stones.put(s.id,s); save(); return s;
    }
    public boolean discover(UUID player, Stone stone) {
        boolean added = data.discoveries.computeIfAbsent(player.toString(), k -> new HashSet<>()).add(stone.id);
        if (added) save(); return added;
    }
    public boolean knows(UUID player, String id) {
        return data.discoveries.getOrDefault(player.toString(), Set.of()).contains(id);
    }
    public List<Stone> known(UUID player) {
        return data.discoveries.getOrDefault(player.toString(),Set.of()).stream().map(data.stones::get)
            .filter(Objects::nonNull).sorted(Comparator.comparing((Stone s)->s.name.toLowerCase(Locale.ROOT)).thenComparing(s->s.id)).toList();
    }
    public void rename(Stone s, String name) { s.name=cleanName(name); save(); }
    public static String cleanName(String name) {
        String result=name.replaceAll("[\\p{Cntrl}§]", "").strip();
        return result.substring(0,Math.min(32,result.length()));
    }
    public void remove(String id) {
        if (data.stones.remove(id) == null) return;
        data.discoveries.values().forEach(set -> set.remove(id)); save();
    }
    private void save() {
        try {
            Files.createDirectories(file.getParent());
            Path tmp=file.resolveSibling(file.getFileName()+".tmp");
            Files.writeString(tmp,GSON.toJson(data),StandardCharsets.UTF_8);
            try { Files.move(tmp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException e) { throw new IllegalStateException("Impossible de sauvegarder les Waystones",e); }
    }
}
