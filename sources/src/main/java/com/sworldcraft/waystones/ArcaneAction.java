package com.sworldcraft.waystones;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record ArcaneAction(int containerId,int action,String text,int page) implements CustomPacketPayload {
    public static final Type<ArcaneAction> TYPE=new Type<>(Identifier.fromNamespaceAndPath(SworldWaystones.ID,"arcane_action"));
    public static final StreamCodec<FriendlyByteBuf,ArcaneAction> CODEC=new StreamCodec<>() {
        public ArcaneAction decode(FriendlyByteBuf buf) { return new ArcaneAction(buf.readVarInt(),buf.readVarInt(),buf.readUtf(64),buf.readVarInt()); }
        public void encode(FriendlyByteBuf buf,ArcaneAction msg) { buf.writeVarInt(msg.containerId);buf.writeVarInt(msg.action);buf.writeUtf(msg.text,64);buf.writeVarInt(msg.page); }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("2").playToServer(TYPE,CODEC,(msg,context)->context.enqueueWork(()->{
            if(context.player() instanceof ServerPlayer p && p.containerMenu.containerId==msg.containerId
                && p.containerMenu instanceof ArcaneMenu menu) menu.action(p,msg.action,msg.text,msg.page);
        }));
    }
}
