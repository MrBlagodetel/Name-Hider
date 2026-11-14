package com.namehider;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Главный экран: всё важное на одной странице. */
public class NHMainScreen extends NHBaseScreen {

    public NHMainScreen(Screen parent) {
        super(Component.literal("Name Hider"), parent);
    }

    private static Component label(String key, String valueKey) {
        return Component.literal(NHLang.f(key, NHLang.t(valueKey)));
    }

    private Component profileLabel() {
        return Component.literal(NHLang.f("profile", NHConfig.p().name, NHConfig.active + 1, NHConfig.profiles.size()));
    }

    private Component masterLabel() {
        return Component.literal(NHLang.f("master", onOff(NHConfig.enabled)))
                .withStyle(NHConfig.enabled ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    private Component myNickLabel() {
        return label("q.mynick", NHConfig.p().hideNick ? "v.nick.on" : "v.nick.off");
    }

    private Component mySkinLabel() {
        return label("q.myskin", NHConfig.p().changeSkin ? "v.skin.on" : "v.skin.off");
    }

    private Component othersNicksLabel() {
        return label("q.othernicks", "ALL".equals(NHConfig.othersNicks) ? "v.others.ALL" : "v.others.OFF");
    }

    private Component othersSkinsLabel() {
        return label("q.otherskins", "ALL".equals(NHConfig.othersSkins) ? "v.others.ALL" : "v.others.OFF");
    }

    private Component myCapeLabel() {
        String m = NHConfig.p().capeMode;
        return label("q.mycape", "REMOVE".equals(m) ? "v.cape.off" : "CUSTOM".equals(m) ? "v.cape.custom" : "v.cape.on");
    }

    private Component othersCapesLabel() {
        return label("q.othercapes", "REMOVE".equals(NHConfig.othersCapes) ? "v.cape.off" : "v.cape.on");
    }

    private void switchProfile(int d) {
        int n = NHConfig.profiles.size();
        NHConfig.active = Math.floorMod(NHConfig.active + d, n);
        NHSkin.reload();
        NHScreens.open(new NHMainScreen(parent));
    }

    private static void nicksChanged() {
        NHText.invalidate();
        NHText.refreshChat();
    }

    @Override
    protected void init() {
        title("title");

        int y = 26;
        addRenderableWidget(Button.builder(Component.literal("<"), b -> switchProfile(-1))
                .bounds(bx(), y, 20, 20).build());
        addRenderableWidget(Button.builder(profileLabel(), b -> switchProfile(1))
                .bounds(bx() + 24, y, W - 48, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> switchProfile(1))
                .bounds(bx() + W - 20, y, 20, 20).build());

        // общий выключатель
        y += 26;
        btn(y, masterLabel(), b -> {
            NHConfig.enabled = !NHConfig.enabled;
            b.setMessage(masterLabel());
            NHSkin.reload();
            NHText.refreshChat();
        });

        // ряд 1: я
        y += 28;
        half(y, false, myNickLabel(), b -> {
            NHConfig.p().hideNick = !NHConfig.p().hideNick;
            b.setMessage(myNickLabel());
            nicksChanged();
        });
        half(y, true, mySkinLabel(), b -> {
            NHConfig.p().changeSkin = !NHConfig.p().changeSkin;
            b.setMessage(mySkinLabel());
            NHSkin.reload();
        });

        // ряд 2: другие игроки
        y += 22;
        half(y, false, othersNicksLabel(), b -> {
            NHConfig.othersNicks = "ALL".equals(NHConfig.othersNicks) ? "OFF" : "ALL";
            b.setMessage(othersNicksLabel());
            nicksChanged();
        });
        half(y, true, othersSkinsLabel(), b -> {
            NHConfig.othersSkins = "ALL".equals(NHConfig.othersSkins) ? "OFF" : "ALL";
            b.setMessage(othersSkinsLabel());
            NHText.invalidate();
        });

        // ряд 3: плащи (вкл = плащ виден, выкл = убран)
        y += 22;
        half(y, false, myCapeLabel(), b -> {
            NHConfig.Profile p = NHConfig.p();
            if ("REMOVE".equals(p.capeMode)) {
                p.capeMode = (p.capePath != null && !p.capePath.isBlank()) ? "CUSTOM" : "KEEP";
            } else {
                p.capeMode = "REMOVE";
            }
            b.setMessage(myCapeLabel());
            NHSkin.reload();
        });
        half(y, true, othersCapesLabel(), b -> {
            NHConfig.othersCapes = "REMOVE".equals(NHConfig.othersCapes) ? "KEEP" : "REMOVE";
            b.setMessage(othersCapesLabel());
        });

        // подробные настройки
        y += 28;
        half(y, false, NHLang.c("menu.my.short"), b -> NHScreens.open(new NHMyScreen(this)));
        half(y, true, NHLang.c("menu.profiles"), b -> NHScreens.open(new NHProfilesScreen(this)));
        y += 22;
        half(y, false, Component.literal(NHLang.f("lang", NHLang.t("lang." + NHConfig.language))), b -> {
            NHConfig.language = next(NHConfig.language, "AUTO", "RU", "EN");
            NHScreens.open(new NHMainScreen(parent));
        });
        half(y, true, NHLang.c("done"), b -> onClose());
    }
}
