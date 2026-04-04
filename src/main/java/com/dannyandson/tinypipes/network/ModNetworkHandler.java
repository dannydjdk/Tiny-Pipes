package com.dannyandson.tinypipes.network;

import com.dannyandson.tinypipes.TinyPipes;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = TinyPipes.MODID)
public class ModNetworkHandler {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(TinyPipes.MODID).versioned("1.0");
        registrar.playToServer(PushItemFilterFlags.TYPE, PushItemFilterFlags.STREAM_CODEC, PushItemFilterFlags::handle);
        registrar.playToServer(PushPipeConnection.TYPE, PushPipeConnection.STREAM_CODEC, PushPipeConnection::handle);
    }

    public static void sendToServer(Object packet) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            if (packet instanceof PushItemFilterFlags pkt)
                connection.send(new ServerboundCustomPayloadPacket(pkt));
            else if (packet instanceof PushPipeConnection pkt)
                connection.send(new ServerboundCustomPayloadPacket(pkt));
        }
    }
}
