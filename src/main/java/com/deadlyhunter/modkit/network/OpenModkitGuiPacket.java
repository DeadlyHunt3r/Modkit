package com.deadlyhunter.modkit.network;

import com.deadlyhunter.modkit.Modkit;
import com.deadlyhunter.modkit.client.ClientHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenModkitGuiPacket() implements CustomPacketPayload {

    public static final Type<OpenModkitGuiPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Modkit.MODID, "open_modkit_gui"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenModkitGuiPacket> STREAM_CODEC =
            StreamCodec.unit(new OpenModkitGuiPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final OpenModkitGuiPacket packet, final IPayloadContext context) {

        context.enqueueWork(ClientHandler::openMainScreen);
    }
}
