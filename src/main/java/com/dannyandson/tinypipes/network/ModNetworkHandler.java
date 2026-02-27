package com.dannyandson.tinypipes.network;

import com.dannyandson.tinypipes.TinyPipes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@SuppressWarnings("removal")
@EventBusSubscriber(modid = TinyPipes.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModNetworkHandler {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(TinyPipes.MODID).versioned("1.0");
        registrar.playToServer(PushItemFilterFlags.TYPE, PushItemFilterFlags.STREAM_CODEC, PushItemFilterFlags::handle);
        registrar.playToServer(PushPipeConnection.TYPE, PushPipeConnection.STREAM_CODEC, PushPipeConnection::handle);
    }

    public static void sendToServer(Object packet) {
        if (packet instanceof PushItemFilterFlags pkt)
            PacketDistributor.sendToServer(pkt);
        else if (packet instanceof PushPipeConnection pkt)
            PacketDistributor.sendToServer(pkt);
    }
}
