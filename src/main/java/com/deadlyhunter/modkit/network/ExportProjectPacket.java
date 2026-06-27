package com.deadlyhunter.modkit.network;

import com.deadlyhunter.modkit.Modkit;
import com.deadlyhunter.modkit.export.ProjectExporter;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ExportProjectPacket(String modName) implements CustomPacketPayload {

    public static final Type<ExportProjectPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Modkit.MODID, "export_project"));

    public static final StreamCodec<ByteBuf, ExportProjectPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64), ExportProjectPacket::modName,
            ExportProjectPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ExportProjectPacket pkt, IPayloadContext context) {
        ServerActions.asOp(context, "\u00a7c[Modkit] No permission to export.", player -> {
            player.sendSystemMessage(Component.literal("\u00a7a[Modkit] Exporting '" + pkt.modName() + "'..."));

            ProjectExporter.ExportResult result = ProjectExporter.export(pkt.modName());
            if (!result.success) {
                player.sendSystemMessage(Component.literal("\u00a7c[Modkit] " + result.message));
                return;
            }

            player.sendSystemMessage(Component.literal(
                    "\u00a7a[Modkit] Exported '" + pkt.modName() + ".jar' ("
                            + result.itemCount + " items, " + result.blockCount + " blocks, "
                            + result.oreCount + " ores, " + result.recipeCount + " recipes, "
                            + result.weaponCount + " weapons, " + result.toolCount + " tools, "
                            + result.armorSetCount + " armor sets)"));

            String absPath = result.jarPath.toAbsolutePath().toString();
            MutableComponent link = Component.literal("\u00a77File: \u00a7r")
                    .append(Component.literal(absPath).withStyle(Style.EMPTY
                            .withColor(ChatFormatting.AQUA)
                            .withUnderlined(true)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, absPath))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    Component.literal("Click to open")))));
            player.sendSystemMessage(link);

            if (!result.warnings.isEmpty()) {
                player.sendSystemMessage(Component.literal("\u00a7e[!] " + result.warnings.size() + " warning(s):"));
                for (String w : result.warnings) {
                    player.sendSystemMessage(Component.literal("\u00a77  - " + w));
                }
            }
        });
    }
}
