package com.namehider;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

abstract class NHBaseScreen extends Screen {
    protected static final int W = 260;
    protected final Screen parent;

    protected NHBaseScreen(Component title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    protected int bx() {
        return this.width / 2 - W / 2;
    }

    protected void title(String key) {
        addRenderableWidget(new StringWidget(bx(), 10, W, 12, NHLang.c(key), this.font));
    }

    protected Button btn(int y, Component label, Button.OnPress press) {
        return addRenderableWidget(Button.builder(label, press).bounds(bx(), y, W, 20).build());
    }

    /** Кнопка в половину ширины (две колонки в одном ряду). */
    protected Button half(int y, boolean right, Component label, Button.OnPress press) {
        int hw = (W - 4) / 2;
        int x = right ? bx() + hw + 4 : bx();
        return addRenderableWidget(Button.builder(label, press).bounds(x, y, hw, 20).build());
    }

    protected EditBox box(int y, int width, String hintKey, String value, int max, Consumer<String> responder) {
        EditBox b = new EditBox(this.font, bx(), y, width, 20, Component.literal(hintKey));
        b.setMaxLength(max);
        b.setHint(NHLang.c(hintKey));
        b.setValue(value == null ? "" : value);
        b.setResponder(responder);
        addRenderableWidget(b);
        return b;
    }

    protected static String next(String cur, String... options) {
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(cur)) return options[(i + 1) % options.length];
        }
        return options[0];
    }

    protected static String onOff(boolean v) {
        return NHLang.t(v ? "on" : "off");
    }

    @Override
    public void onClose() {
        NHConfig.save();
        NHText.invalidate();
        NHText.refreshChat();
        NHScreens.open(parent);
    }
}
