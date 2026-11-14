package com.namehider;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class NHScreens {
    private NHScreens() {}

    /** Открывает экран. С Minecraft 26.2 это делается через Minecraft.gui. */
    public static void open(Screen screen) {
        Minecraft.getInstance().gui.setScreen(screen);
    }
}
