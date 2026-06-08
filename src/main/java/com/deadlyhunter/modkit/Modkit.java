package com.deadlyhunter.modkit;

import com.deadlyhunter.modkit.command.ModkitCommand;
import com.deadlyhunter.modkit.content.ProjectRegistry;
import com.deadlyhunter.modkit.core.AuthorConfig;
import com.deadlyhunter.modkit.core.WorkspaceManager;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Modkit.MODID)
public class Modkit {

    public static final String MODID = "modkit";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Modkit() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        WorkspaceManager.init();
        AuthorConfig.init();

        com.deadlyhunter.modkit.network.ModNetworking.register();

        ProjectRegistry.scanAndPrepareRegistries(modEventBus);
        MinecraftForge.EVENT_BUS.register(ProjectRegistry.class);

        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
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
