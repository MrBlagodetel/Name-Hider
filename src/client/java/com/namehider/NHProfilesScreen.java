package com.namehider;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Управление профилями: переключить, переименовать, создать копию, удалить. */
public class NHProfilesScreen extends NHBaseScreen {

    public NHProfilesScreen(Screen parent) {
        super(Component.literal("Name Hider"), parent);
    }

    private Component profileLabel() {
        return Component.literal(NHLang.f("profile", NHConfig.p().name, NHConfig.active + 1, NHConfig.profiles.size()));
    }

    private void reopen() {
        NHSkin.reload();
        NHScreens.open(new NHProfilesScreen(parent));
    }

    private void switchProfile(int d) {
        NHConfig.active = Math.floorMod(NHConfig.active + d, NHConfig.profiles.size());
        reopen();
    }

    @Override
    protected void init() {
        NHConfig.Profile p = NHConfig.p();
        title("profiles.title");

        int y = 28;
        addRenderableWidget(Button.builder(Component.literal("<"), b -> switchProfile(-1))
                .bounds(bx(), y, 20, 20).build());
        addRenderableWidget(Button.builder(profileLabel(), b -> switchProfile(1))
                .bounds(bx() + 24, y, W - 48, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> switchProfile(1))
                .bounds(bx() + W - 20, y, 20, 20).build());

        y += 24;
        box(y, W, "profile.hint", p.name, 24, s -> { if (!s.isBlank()) p.name = s; });

        y += 24;
        half(y, false, NHLang.c("profile.new"), b -> {
            NHConfig.Profile np = NHConfig.copy(NHConfig.p());
            np.name = NHLang.t("profile.default") + " " + (NHConfig.profiles.size() + 1);
            NHConfig.profiles.add(np);
            NHConfig.active = NHConfig.profiles.size() - 1;
            reopen();
        });
        half(y, true, NHLang.c("profile.delete"), b -> {
            if (NHConfig.profiles.size() > 1) {
                NHConfig.profiles.remove(NHConfig.active);
                NHConfig.active = Math.min(NHConfig.active, NHConfig.profiles.size() - 1);
                reopen();
            }
        });

        btn(this.height - 28, NHLang.c("back"), b -> onClose());
    }
}
