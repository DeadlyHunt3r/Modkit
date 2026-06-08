package com.deadlyhunter.modkit.network;

import com.deadlyhunter.modkit.Modkit;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetworking {

    private static final String PROTOCOL_VERSION = "1";
    private static int packetId = 0;

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(Modkit.MODID, "main"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();

    private ModNetworking() {}

    public static void register() {

        CHANNEL.messageBuilder(OpenModkitGuiPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenModkitGuiPacket::encode)
                .decoder(OpenModkitGuiPacket::decode)
                .consumerMainThread(OpenModkitGuiPacket::handle)
                .add();
        CHANNEL.messageBuilder(SaveItemPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SaveItemPacket::encode)
                .decoder(SaveItemPacket::decode)
                .consumerMainThread(SaveItemPacket::handle)
                .add();
        CHANNEL.messageBuilder(DeleteItemPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DeleteItemPacket::encode)
                .decoder(DeleteItemPacket::decode)
                .consumerMainThread(DeleteItemPacket::handle)
                .add();
        CHANNEL.messageBuilder(SetTexturePacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SetTexturePacket::encode)
                .decoder(SetTexturePacket::decode)
                .consumerMainThread(SetTexturePacket::handle)
                .add();
        CHANNEL.messageBuilder(SaveBlockPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SaveBlockPacket::encode)
                .decoder(SaveBlockPacket::decode)
                .consumerMainThread(SaveBlockPacket::handle)
                .add();
        CHANNEL.messageBuilder(DeleteBlockPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DeleteBlockPacket::encode)
                .decoder(DeleteBlockPacket::decode)
                .consumerMainThread(DeleteBlockPacket::handle)
                .add();
        CHANNEL.messageBuilder(SetBlockTexturePacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SetBlockTexturePacket::encode)
                .decoder(SetBlockTexturePacket::decode)
                .consumerMainThread(SetBlockTexturePacket::handle)
                .add();
        CHANNEL.messageBuilder(SaveOrePacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SaveOrePacket::encode)
                .decoder(SaveOrePacket::decode)
                .consumerMainThread(SaveOrePacket::handle)
                .add();
        CHANNEL.messageBuilder(DeleteOrePacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DeleteOrePacket::encode)
                .decoder(DeleteOrePacket::decode)
                .consumerMainThread(DeleteOrePacket::handle)
                .add();
        CHANNEL.messageBuilder(SaveRecipePacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SaveRecipePacket::encode)
                .decoder(SaveRecipePacket::decode)
                .consumerMainThread(SaveRecipePacket::handle)
                .add();
        CHANNEL.messageBuilder(DeleteRecipePacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DeleteRecipePacket::encode)
                .decoder(DeleteRecipePacket::decode)
                .consumerMainThread(DeleteRecipePacket::handle)
                .add();
        CHANNEL.messageBuilder(SaveWeaponPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SaveWeaponPacket::encode)
                .decoder(SaveWeaponPacket::decode)
                .consumerMainThread(SaveWeaponPacket::handle)
                .add();
        CHANNEL.messageBuilder(DeleteWeaponPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DeleteWeaponPacket::encode)
                .decoder(DeleteWeaponPacket::decode)
                .consumerMainThread(DeleteWeaponPacket::handle)
                .add();
        CHANNEL.messageBuilder(SaveToolPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SaveToolPacket::encode)
                .decoder(SaveToolPacket::decode)
                .consumerMainThread(SaveToolPacket::handle)
                .add();
        CHANNEL.messageBuilder(DeleteToolPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DeleteToolPacket::encode)
                .decoder(DeleteToolPacket::decode)
                .consumerMainThread(DeleteToolPacket::handle)
                .add();
        CHANNEL.messageBuilder(SetAuthorPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SetAuthorPacket::encode)
                .decoder(SetAuthorPacket::decode)
                .consumerMainThread(SetAuthorPacket::handle)
                .add();
        CHANNEL.messageBuilder(CreateWorkspacePacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CreateWorkspacePacket::encode)
                .decoder(CreateWorkspacePacket::decode)
                .consumerMainThread(CreateWorkspacePacket::handle)
                .add();
        CHANNEL.messageBuilder(DeleteWorkspacePacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DeleteWorkspacePacket::encode)
                .decoder(DeleteWorkspacePacket::decode)
                .consumerMainThread(DeleteWorkspacePacket::handle)
                .add();
        CHANNEL.messageBuilder(UpdateProjectInfoPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(UpdateProjectInfoPacket::encode)
                .decoder(UpdateProjectInfoPacket::decode)
                .consumerMainThread(UpdateProjectInfoPacket::handle)
                .add();
        CHANNEL.messageBuilder(ExportProjectPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ExportProjectPacket::encode)
                .decoder(ExportProjectPacket::decode)
                .consumerMainThread(ExportProjectPacket::handle)
                .add();

        Modkit.LOGGER.info("[Modkit] Network channel registered.");
    }
}
