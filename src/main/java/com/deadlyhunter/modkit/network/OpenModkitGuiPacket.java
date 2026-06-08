package com.deadlyhunter.modkit.network;

import com.deadlyhunter.modkit.client.ClientHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenModkitGuiPacket {

    public OpenModkitGuiPacket() {}

    public static void encode(OpenModkitGuiPacket pkt, FriendlyByteBuf buf) {
    }

    public static OpenModkitGuiPacket decode(FriendlyByteBuf buf) {
        return new OpenModkitGuiPacket();
    }

    public static void handle(OpenModkitGuiPacket pkt, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ClientHandler.openMainScreen();
        ctx.setPacketHandled(true);
    }
}
