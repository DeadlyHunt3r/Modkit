package com.deadlyhunter.modkit.network;

import com.deadlyhunter.modkit.export.ProjectExporter;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class ExportProjectPacket {

    private final String modName;

    public ExportProjectPacket(String modName) {
        this.modName = modName;
    }

    public static void encode(ExportProjectPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.modName, 64);
    }

    public static ExportProjectPacket decode(FriendlyByteBuf buf) {
        return new ExportProjectPacket(buf.readUtf(64));
    }

    public static void handle(ExportProjectPacket pkt, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ServerPlayer player = ctx.getSender();
        if (player == null) { ctx.setPacketHandled(true); return; }

        if (!player.hasPermissions(2)) {
            player.sendSystemMessage(Component.literal("§c[Modkit] No permission to export."));
            ctx.setPacketHandled(true);
            return;
        }

        player.sendSystemMessage(Component.literal("§a[Modkit] Exporting '" + pkt.modName + "'..."));
        ProjectExporter.ExportResult result = ProjectExporter.export(pkt.modName);

        if (!result.success) {
            player.sendSystemMessage(Component.literal("§c[Modkit] " + result.message));
            ctx.setPacketHandled(true);
            return;
        }

        player.sendSystemMessage(Component.literal(
                "§a[Modkit] Exported '" + pkt.modName + ".jar' ("
                        + result.itemCount + " items, " + result.blockCount + " blocks, "
                        + result.oreCount + " ores, " + result.recipeCount + " recipes, "
                        + result.weaponCount + " weapons, " + result.toolCount + " tools, "
                        + result.armorSetCount + " armor sets)"));

        String absPath = result.jarPath.toAbsolutePath().toString();
        MutableComponent link = Component.literal("§7File: §r")
                .append(Component.literal(absPath).withStyle(Style.EMPTY
                        .withColor(ChatFormatting.AQUA)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, absPath))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.literal("Click to open")))));
        player.sendSystemMessage(link);

        if (!result.warnings.isEmpty()) {
            player.sendSystemMessage(Component.literal("§e[!] " + result.warnings.size() + " warning(s):"));
            for (String w : result.warnings) {
                player.sendSystemMessage(Component.literal("§7  - " + w));
            }
        }

        ctx.setPacketHandled(true);
    }
}
