package com.deadlyhunter.modkit.client;

import com.deadlyhunter.modkit.client.screen.ModkitMainScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public final class ClientHandler {

    private ClientHandler() {}

    public static void openMainScreen() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientHandler::openMainScreenImpl);
    }

    private static void openMainScreenImpl() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> mc.setScreen(new ModkitMainScreen()));
    }
}
