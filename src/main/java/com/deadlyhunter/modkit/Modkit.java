package com.deadlyhunter.modkit;

import com.deadlyhunter.modkit.command.ModkitCommand;
import com.deadlyhunter.modkit.content.ProjectRegistry;
import com.deadlyhunter.modkit.core.AuthorConfig;
import com.deadlyhunter.modkit.core.WorkspaceManager;
import com.deadlyhunter.modkit.network.ModNetworking;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

@Mod(Modkit.MODID)
public class Modkit {

    public static final String MODID = "modkit";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Modkit(IEventBus modEventBus, ModContainer modContainer) {
        WorkspaceManager.init();
        AuthorConfig.init();


        modEventBus.addListener(ModNetworking::register);


        ProjectRegistry.scanAndPrepareRegistries(modEventBus);

        modEventBus.addListener(this::commonSetup);


        NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("[Modkit] Common setup complete.");
        LOGGER.info("[Modkit] {} project(s) loaded.", ProjectRegistry.getProjects().size());
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ModkitCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("[Modkit] Server starting.");
    }
}
