package com.namehider;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public class NHMyScreen extends NHBaseScreen {
    private final NHConfig.Profile p = NHConfig.p();
    private EditBox skinBox, capeBox;
    private Button capeBtn;
    private StringWidget status;

    public NHMyScreen(Screen parent) {
        super(Component.literal("Name Hider"), parent);
    }

    private Component hideLabel() { return Component.literal(NHLang.f("my.hide", onOff(p.hideNick))); }
    private Component skinLabel() { return Component.literal(NHLang.f("my.skin", onOff(p.changeSkin))); }
    private Component modelLabel() { return Component.literal(NHLang.f("my.model", NHLang.t("model." + p.model))); }
    private Component capeLabel() { return Component.literal(NHLang.f("my.cape", NHLang.t("cape." + p.capeMode))); }

    private void refresh() {
        NHSkin.reload();
        if (status != null) status.setMessage(Component.literal(NHSkin.statusText()));
    }

    @Override
    protected void init() {
        title("my.title");
        int y = 28;

        btn(y, hideLabel(), b -> {
            p.hideNick = !p.hideNick;
            b.setMessage(hideLabel());
            NHText.invalidate();
        });

        y += 24;
        box(y, W, "my.nick.hint", p.customNick, 32, s -> p.customNick = s);

        y += 24;
        btn(y, skinLabel(), b -> {
            p.changeSkin = !p.changeSkin;
            b.setMessage(skinLabel());
            refresh();
        });

        y += 24;
        skinBox = box(y, W - 84, "my.skinpath.hint", p.skinPath, 260, s -> {
            p.skinPath = s;
            String t = s.trim().toLowerCase(Locale.ROOT);
            if (t.isEmpty() || t.endsWith(".png")) refresh();
        });
        addRenderableWidget(Button.builder(NHLang.c("pick"), b -> {
            String r = NHFiles.pickPng(NHLang.t("pick.title"));
            if (r != null && !r.isBlank()) skinBox.setValue(r);
        }).bounds(bx() + W - 80, y, 80, 20).build());

        y += 24;
        btn(y, modelLabel(), b -> {
            p.model = next(p.model, "AUTO", "WIDE", "SLIM");
            b.setMessage(modelLabel());
        });

        y += 24;
        capeBtn = btn(y, capeLabel(), b -> {
            p.capeMode = next(p.capeMode, "KEEP", "REMOVE", "CUSTOM");
            b.setMessage(capeLabel());
            refresh();
        });

        y += 24;
        capeBox = box(y, W - 84, "my.capepath.hint", p.capePath, 260, s -> {
            p.capePath = s;
            String t = s.trim().toLowerCase(Locale.ROOT);
            if (t.isEmpty() || t.endsWith(".png")) refresh();
        });
        addRenderableWidget(Button.builder(NHLang.c("pick"), b -> {
            String r = NHFiles.pickPng(NHLang.t("pick.title"));
            if (r != null && !r.isBlank()) capeBox.setValue(r);
        }).bounds(bx() + W - 80, y, 80, 20).build());

        y += 26;
        status = new StringWidget(bx(), y, W, 12, Component.literal(NHSkin.statusText()), this.font);
        addRenderableWidget(status);
        y += 14;
        addRenderableWidget(new StringWidget(bx(), y, W, 12, NHLang.c("hint.drag"), this.font));

        btn(this.height - 28, NHLang.c("done"), b -> onClose());
    }

    /** Перетаскивание PNG в окно: имя с "cape" = плащ, иначе скин. */
    public void onFilesDrop(List<Path> paths) {
        if (paths == null || paths.isEmpty() || skinBox == null || capeBox == null) return;
        Path f = paths.get(0);
        String full = f.toAbsolutePath().toString();
        if (f.getFileName().toString().toLowerCase(Locale.ROOT).contains("cape")) {
            p.capeMode = "CUSTOM";
            if (capeBtn != null) capeBtn.setMessage(capeLabel());
            capeBox.setValue(full);
        } else {
            skinBox.setValue(full);
        }
    }
}
