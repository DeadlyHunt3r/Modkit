package com.deadlyhunter.modkit.client;

import net.minecraft.client.Minecraft;
import com.deadlyhunter.modkit.client.screen.ModkitMainScreen;

public final class ClientHandler {

    private ClientHandler() {}

    public static void openMainScreen() {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> mc.setScreen(new ModkitMainScreen()));
    }
}
