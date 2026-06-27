package com.deadlyhunter.modkit.command;

import com.deadlyhunter.modkit.core.AuthorConfig;
import com.deadlyhunter.modkit.core.ProjectInfo;
import com.deadlyhunter.modkit.core.WorkspaceManager;
import com.deadlyhunter.modkit.export.ProjectExporter;
import com.deadlyhunter.modkit.network.OpenModkitGuiPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class ModkitCommand {

    private ModkitCommand() {}

    private static final SuggestionProvider<CommandSourceStack> WORKSPACE_SUGGESTIONS =
            (ctx, builder) -> suggestWorkspaces(builder);

    private static CompletableFuture<Suggestions> suggestWorkspaces(SuggestionsBuilder builder) {
        for (String name : WorkspaceManager.listWorkspaces()) {
            if (name.toLowerCase().startsWith(builder.getRemainingLowerCase())) {
                builder.suggest(name);
            }
        }
        return builder.buildFuture();
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("modkit")
                        .executes(ModkitCommand::openGui)

                        .then(Commands.literal("help")
                                .executes(ModkitCommand::help))
                        .then(Commands.literal("setauthor")
                                .then(Commands.argument("prefix", StringArgumentType.word())
                                        .executes(ModkitCommand::setAuthor)))
                        .then(Commands.literal("create")
                                .then(Commands.argument("modname", StringArgumentType.word())
                                        .executes(ModkitCommand::create)))
                        .then(Commands.literal("list")
                                .executes(ModkitCommand::list))
                        .then(Commands.literal("export")
                                .then(Commands.argument("modname", StringArgumentType.word())
                                        .suggests(WORKSPACE_SUGGESTIONS)
                                        .executes(ModkitCommand::export)))
        );
    }

    private static int openGui(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            sendError(source, "GUI can only be opened by a player.");
            return 0;
        }
        PacketDistributor.sendToPlayer(player, new OpenModkitGuiPacket());
        return 1;
    }

    private static int help(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        send(source, "\u00a7e=== Modkit Shortcuts ===");
        send(source, "\u00a77/modkit\u00a7r - Open the visual editor");
        send(source, "\u00a77/modkit setauthor <prefix>\u00a7r - Quick set author");
        send(source, "\u00a77/modkit create <modname>\u00a7r - Quick create workspace");
        send(source, "\u00a77/modkit list\u00a7r - List workspaces");
        send(source, "\u00a77/modkit export <modname>\u00a7r - Export to .jar");
        return 1;
    }

    private static int setAuthor(CommandContext<CommandSourceStack> ctx) {
        String prefix = StringArgumentType.getString(ctx, "prefix");
        CommandSourceStack source = ctx.getSource();
        if (!AuthorConfig.isValid(prefix)) {
            sendError(source, AuthorConfig.getValidationHint());
            return 0;
        }
        String previous = AuthorConfig.getAuthor();
        AuthorConfig.setAuthor(prefix);
        if (previous == null) sendSuccess(source, "Author prefix set to '" + prefix + "'.");
        else sendSuccess(source, "Author prefix changed: '" + previous + "' -> '" + prefix + "'.");
        return 1;
    }

    private static int create(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String modName = StringArgumentType.getString(ctx, "modname");
        if (!AuthorConfig.isSet()) {
            sendError(source, "Set an author first: /modkit setauthor <prefix>");
            return 0;
        }
        WorkspaceManager.CreateResult result = WorkspaceManager.create(AuthorConfig.getAuthor(), modName);
        if (!result.success) { sendError(source, result.message); return 0; }
        ProjectInfo info = result.info;
        sendSuccess(source, "Created workspace '" + info.modName + "'");
        send(source, "\u00a77Mod ID: \u00a7f" + info.modId);
        sendClickablePath(source, "Location: ", WorkspaceManager.getWorkspacePath(info.modName));
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        List<String> workspaces = WorkspaceManager.listWorkspaces();
        if (workspaces.isEmpty()) {
            send(source, "\u00a77No workspaces yet. Use /modkit to open the GUI.");
            return 1;
        }
        send(source, "\u00a7e=== Your Workspaces (" + workspaces.size() + ") ===");
        for (String name : workspaces) {
            ProjectInfo info = WorkspaceManager.loadProject(name);
            if (info != null) send(source, "\u00a77- \u00a7f" + name + " \u00a78(" + info.modId + ")");
            else send(source, "\u00a77- \u00a7f" + name + " \u00a7c(corrupt)");
        }
        return 1;
    }

    private static int export(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String modName = StringArgumentType.getString(ctx, "modname");
        if (!WorkspaceManager.exists(modName)) {
            sendError(source, "No workspace named '" + modName + "' found.");
            return 0;
        }
        sendSuccess(source, "Exporting '" + modName + "'...");
        ProjectExporter.ExportResult result = ProjectExporter.export(modName);
        if (!result.success) { sendError(source, result.message); return 0; }
        sendSuccess(source, "Exported '" + modName + ".jar' ("
                + result.itemCount + " items, " + result.blockCount + " blocks, "
                + result.oreCount + " ores, " + result.recipeCount + " recipes, "
                + result.weaponCount + " weapons, " + result.toolCount + " tools, "
                + result.armorSetCount + " armor sets)");
        sendClickablePath(source, "File: ", result.jarPath);
        sendClickablePath(source, "Folder: ", result.jarPath.getParent());
        if (!result.warnings.isEmpty()) {
            send(source, "\u00a7e[!] " + result.warnings.size() + " warning(s):");
            for (String w : result.warnings) send(source, "\u00a77  - " + w);
        }
        send(source, "\u00a77Drop the .jar into your \u00a7fmods\u00a77 folder to use the mod.");
        return 1;
    }

    private static void send(CommandSourceStack source, String text) {
        source.sendSuccess(() -> Component.literal(text), false);
    }

    private static void sendSuccess(CommandSourceStack source, String text) {
        source.sendSuccess(() -> Component.literal("\u00a7a[Modkit] \u00a7r" + text), false);
    }

    private static void sendError(CommandSourceStack source, String text) {
        source.sendFailure(Component.literal("\u00a7c[Modkit] " + text).withStyle(ChatFormatting.RED));
    }

    private static void sendClickablePath(CommandSourceStack source, String prefix, Path path) {
        String pathStr = path.toAbsolutePath().toString();
        MutableComponent line = Component.literal("\u00a77" + prefix + "\u00a7r");
        MutableComponent link = Component.literal(pathStr)
                .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.AQUA)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, pathStr))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.literal("Click to open"))));
        line.append(link);
        source.sendSuccess(() -> line, false);
    }
}
