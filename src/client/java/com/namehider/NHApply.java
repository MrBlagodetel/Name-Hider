package com.namehider;

import net.minecraft.client.Minecraft;
import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Общая логика подмены скина/плаща/головы для миксинов. */
public final class NHApply {
    private NHApply() {}

    /** Настоящий (скачанный) скин моего аккаунта, пока мы его не подменили. По нему находим NPC с моим скином и головы. */
    private static volatile ClientAsset.Texture realBody;

    /** Настоящие скины других игроков -> их ник. Нужны, чтобы менять и головы, которые рисуются мимо getSkin(). */
    private static final Map<ClientAsset.Texture, String> othersBodies =
            Collections.synchronizedMap(new LinkedHashMap<>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<ClientAsset.Texture, String> e) {
                    return size() > 1024;
                }
            });

    /** Пока внутри apply(), хук на PlayerSkin.body() должен отдавать настоящую текстуру. */
    private static final ThreadLocal<int[]> DEPTH = ThreadLocal.withInitial(() -> new int[1]);

    public static PlayerSkin apply(PlayerSkin orig, UUID id, String name) {
        if (orig == null) return null;
        int[] d = DEPTH.get();
        d[0]++;
        try {
            return apply0(orig, id, name);
        } finally {
            d[0]--;
        }
    }

    private static PlayerSkin apply0(PlayerSkin orig, UUID id, String name) {
        boolean mine = NHSkin.isLocal(id, name);
        ClientAsset.Texture origBody = orig.body();
        boolean real = origBody != null && !(origBody instanceof ClientAsset.ResourceTexture);
        if (mine) {
            // стандартные скины (Steve/Alex) не запоминаем: иначе "мной" стали бы все игроки с таким же скином
            if (real) realBody = origBody;
        } else if (hasMySkin(origBody)) {
            // NPC или другой игрок, у которого скин моего аккаунта: скрываем как свой
            mine = true;
        } else if (real && name != null) {
            othersBodies.put(origBody, name.toLowerCase(Locale.ROOT));
        }

        if (!NHConfig.enabled) return orig;

        ClientAsset.Texture body = origBody;
        ClientAsset.Texture cape = orig.cape();
        ClientAsset.Texture elytra = orig.elytra();
        PlayerModelType model = orig.model();
        boolean changed = false;

        if (mine) {
            NHConfig.Profile p = NHConfig.p();
            if (p.changeSkin) {
                ClientAsset.Texture t = NHSkin.ownSkin();
                if (t != null) {
                    body = t;
                    changed = true;
                    if ("WIDE".equals(p.model)) model = PlayerModelType.WIDE;
                    else if ("SLIM".equals(p.model)) model = PlayerModelType.SLIM;
                }
            }
            if ("REMOVE".equals(p.capeMode)) {
                cape = null;
                elytra = null;
                changed = true;
            } else if ("CUSTOM".equals(p.capeMode)) {
                ClientAsset.Texture c = NHSkin.ownCape();
                if (c != null) {
                    cape = c;
                    elytra = c;
                    changed = true;
                }
            }
        } else {
            if (NHText.matchesMode(name, NHConfig.othersSkins, NHConfig.othersSkinList)) {
                ClientAsset.Texture t = NHSkin.othersSkin();
                if (t != null) {
                    body = t;
                    changed = true;
                }
            }
            if ("REMOVE".equals(NHConfig.othersCapes)) {
                cape = null;
                elytra = null;
                changed = true;
            }
        }
        return changed ? new PlayerSkin(body, cape, elytra, model, orig.secure()) : orig;
    }

    /**
     * Вызывается из PlayerSkin.body(): любой код, который рисует голову или тело по скачанному скину
     * (список игроков, меню, головы игроков), получает подменённую текстуру, даже если он не спрашивал getSkin().
     */
    public static ClientAsset.Texture swapBody(ClientAsset.Texture t) {
        if (t == null || DEPTH.get()[0] > 0 || t instanceof ClientAsset.ResourceTexture) return t;
        if (!NHConfig.enabled) return t;
        ClientAsset.Texture rb = realBody;
        if (rb != null && rb.equals(t)) {
            if (NHConfig.p().changeSkin) {
                ClientAsset.Texture own = NHSkin.ownSkin();
                if (own != null) return own;
            }
            return t;
        }
        String name = othersBodies.get(t);
        if (name != null && NHText.matchesMode(name, NHConfig.othersSkins, NHConfig.othersSkinList)) {
            ClientAsset.Texture o = NHSkin.othersSkin();
            if (o != null) return o;
        }
        return t;
    }

    /** Совпадает ли скачанный скин с настоящим скином моего аккаунта. */
    private static boolean hasMySkin(ClientAsset.Texture body) {
        if (body == null || body instanceof ClientAsset.ResourceTexture) return false;
        ClientAsset.Texture rb = realBody;
        if (rb == null) {
            // мой скин ещё не запрашивался: спрашиваем у своего игрока (это заполнит realBody)
            try {
                var self = Minecraft.getInstance().player;
                if (self != null) self.getSkin();
            } catch (Throwable ignored) {
            }
            rb = realBody;
        }
        return rb != null && rb.equals(body);
    }
}
