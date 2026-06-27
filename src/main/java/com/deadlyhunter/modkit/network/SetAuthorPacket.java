package com.deadlyhunter.modkit.network;

import com.deadlyhunter.modkit.core.AuthorConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.deadlyhunter.modkit.Modkit;

public record SetAuthorPacket(String prefix) implements CustomPacketPayload {

    public static final Type<SetAuthorPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Modkit.MODID, "set_author"));

    public static final StreamCodec<ByteBuf, SetAuthorPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(32), SetAuthorPacket::prefix,
            SetAuthorPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetAuthorPacket pkt, IPayloadContext context) {
        ServerActions.asOp(context, "\u00a7c[Modkit] No permission to change author.", player -> {
            if (!AuthorConfig.isValid(pkt.prefix())) {
                player.sendSystemMessage(Component.literal("\u00a7c[Modkit] " + AuthorConfig.getValidationHint()));
            } else {
                AuthorConfig.setAuthor(pkt.prefix());
                player.sendSystemMessage(Component.literal("\u00a7a[Modkit] Author set to '" + pkt.prefix() + "'."));
            }
        });
    }
}
