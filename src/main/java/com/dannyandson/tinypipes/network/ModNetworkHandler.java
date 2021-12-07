package com.dannyandson.tinypipes.network;

import com.dannyandson.tinypipes.TinyPipes;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public class ModNetworkHandler {
    private static SimpleChannel INSTANCE;
    private static int ID = 0;
    private static final String PROTOCOL_VERSION = "1.0 ";

    private static int nextID() {
        return ID++;
    }

    public static void registerMessages() {

        INSTANCE = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(TinyPipes.MODID, "tinypipes"),
                () -> PROTOCOL_VERSION,
                PROTOCOL_VERSION::equals,
                PROTOCOL_VERSION::equals);

        INSTANCE.messageBuilder(PushItemFilterFlags.class,nextID())
                .encoder(PushItemFilterFlags::toBytes)
                .decoder(PushItemFilterFlags::new)
                .consumer(PushItemFilterFlags::handle)
                .add();
    }
    public static void sendToClient(Object packet, ServerPlayerEntity player) {
        INSTANCE.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }
    public static void sendToServer(Object packet) {
        INSTANCE.sendToServer(packet);
    }

}
