package com.deadlyhunter.modkit.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetworking {

    private static final String PROTOCOL_VERSION = "1";

    private ModNetworking() {}

    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        registrar.playToClient(OpenModkitGuiPacket.TYPE, OpenModkitGuiPacket.STREAM_CODEC, OpenModkitGuiPacket::handle);

        registrar.playToServer(SaveItemPacket.TYPE,           SaveItemPacket.STREAM_CODEC,           SaveItemPacket::handle);
        registrar.playToServer(DeleteItemPacket.TYPE,         DeleteItemPacket.STREAM_CODEC,         DeleteItemPacket::handle);
        registrar.playToServer(SetTexturePacket.TYPE,         SetTexturePacket.STREAM_CODEC,         SetTexturePacket::handle);
        registrar.playToServer(SaveBlockPacket.TYPE,          SaveBlockPacket.STREAM_CODEC,          SaveBlockPacket::handle);
        registrar.playToServer(DeleteBlockPacket.TYPE,        DeleteBlockPacket.STREAM_CODEC,        DeleteBlockPacket::handle);
        registrar.playToServer(SetBlockTexturePacket.TYPE,    SetBlockTexturePacket.STREAM_CODEC,    SetBlockTexturePacket::handle);
        registrar.playToServer(SaveOrePacket.TYPE,            SaveOrePacket.STREAM_CODEC,            SaveOrePacket::handle);
        registrar.playToServer(DeleteOrePacket.TYPE,          DeleteOrePacket.STREAM_CODEC,          DeleteOrePacket::handle);
        registrar.playToServer(SaveRecipePacket.TYPE,         SaveRecipePacket.STREAM_CODEC,         SaveRecipePacket::handle);
        registrar.playToServer(DeleteRecipePacket.TYPE,       DeleteRecipePacket.STREAM_CODEC,       DeleteRecipePacket::handle);
        registrar.playToServer(SaveOverridePacket.TYPE,       SaveOverridePacket.STREAM_CODEC,       SaveOverridePacket::handle);
        registrar.playToServer(DeleteOverridePacket.TYPE,     DeleteOverridePacket.STREAM_CODEC,     DeleteOverridePacket::handle);
        registrar.playToServer(SaveWeaponPacket.TYPE,         SaveWeaponPacket.STREAM_CODEC,         SaveWeaponPacket::handle);
        registrar.playToServer(DeleteWeaponPacket.TYPE,       DeleteWeaponPacket.STREAM_CODEC,       DeleteWeaponPacket::handle);
        registrar.playToServer(SaveToolPacket.TYPE,           SaveToolPacket.STREAM_CODEC,           SaveToolPacket::handle);
        registrar.playToServer(DeleteToolPacket.TYPE,         DeleteToolPacket.STREAM_CODEC,         DeleteToolPacket::handle);
        registrar.playToServer(SaveArmorPacket.TYPE,          SaveArmorPacket.STREAM_CODEC,          SaveArmorPacket::handle);
        registrar.playToServer(DeleteArmorPacket.TYPE,        DeleteArmorPacket.STREAM_CODEC,        DeleteArmorPacket::handle);
        registrar.playToServer(SetArmorTexturePacket.TYPE,    SetArmorTexturePacket.STREAM_CODEC,    SetArmorTexturePacket::handle);
        registrar.playToServer(SetAuthorPacket.TYPE,          SetAuthorPacket.STREAM_CODEC,          SetAuthorPacket::handle);
        registrar.playToServer(CreateWorkspacePacket.TYPE,    CreateWorkspacePacket.STREAM_CODEC,    CreateWorkspacePacket::handle);
        registrar.playToServer(DeleteWorkspacePacket.TYPE,    DeleteWorkspacePacket.STREAM_CODEC,    DeleteWorkspacePacket::handle);
        registrar.playToServer(UpdateProjectInfoPacket.TYPE,  UpdateProjectInfoPacket.STREAM_CODEC,  UpdateProjectInfoPacket::handle);
        registrar.playToServer(ExportProjectPacket.TYPE,      ExportProjectPacket.STREAM_CODEC,      ExportProjectPacket::handle);
    }
}
