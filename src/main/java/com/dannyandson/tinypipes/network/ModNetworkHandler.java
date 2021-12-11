package com.dannyandson.tinypipes.network;

import com.dannyandson.tinypipes.TinyPipes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fmllegacy.network.NetworkDirection;
import net.minecraftforge.fmllegacy.network.NetworkRegistry;
import net.minecraftforge.fmllegacy.network.simple.SimpleChannel;

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

        INSTANCE.messageBuilder(PushPipeConnection.class,nextID())
                .encoder(PushPipeConnection::toBytes)
                .decoder(PushPipeConnection::new)
                .consumer(PushPipeConnection::handle)
                .add();

    }

    public static void sendToClient(Object packet, ServerPlayer player) {
        INSTANCE.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static void sendToServer(Object packet) {
        INSTANCE.sendToServer(packet);
    }

}
