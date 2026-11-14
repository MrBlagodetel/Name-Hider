package com.namehider;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public final class NHSkin {
    private static final String DEFAULT_RES = "/assets/namehider/skin/default.png";
    private static final Identifier ID_SKIN = Identifier.fromNamespaceAndPath("namehider", "own_skin");
    private static final Identifier ID_CAPE = Identifier.fromNamespaceAndPath("namehider", "own_cape");
    private static final Identifier ID_OTHERS = Identifier.fromNamespaceAndPath("namehider", "others_skin");

    private record Result(ClientAsset.Texture tex, String code) {}

    private static ClientAsset.Texture skinTex, capeTex, othersTex;
    private static String skinCode = "none", capeCode = "none";
    private static boolean dirty = true;

    private NHSkin() {}

    public static void markDirty() { dirty = true; }

    public static void reload() {
        dirty = true;
        ensure();
        NHText.invalidate();
    }

    private static void ensure() {
        if (!dirty) return;
        dirty = false;
        NHConfig.Profile p = NHConfig.p();
        Result s = p.changeSkin ? load(ID_SKIN, p.skinPath, DEFAULT_RES, false) : new Result(null, "off");
        boolean customCape = "CUSTOM".equals(p.capeMode) && p.capePath != null && !p.capePath.isBlank();
        Result c = customCape ? load(ID_CAPE, p.capePath, null, true) : new Result(null, "none");
        Result o = load(ID_OTHERS, "", DEFAULT_RES, false);
        skinTex = s.tex(); skinCode = s.code();
        capeTex = c.tex(); capeCode = c.code();
        othersTex = o.tex();
    }

    public static ClientAsset.Texture ownSkin() { ensure(); return skinTex; }
    public static ClientAsset.Texture ownCape() { ensure(); return capeTex; }
    public static ClientAsset.Texture othersSkin() { ensure(); return othersTex; }

    public static String statusText() {
        ensure();
        String s = NHLang.t("st.skin." + skinCode);
        String c = "none".equals(capeCode) ? NHLang.t("st.cape.none") : NHLang.t("st.cape." + capeCode);
        return s + "  |  " + c;
    }

    public static boolean isLocal(UUID id, String name) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.getUser() == null) return false;
            if (id != null && id.equals(mc.getUser().getProfileId())) return true;
            return name != null && name.equalsIgnoreCase(mc.getUser().getName());
        } catch (Throwable e) {
            return false;
        }
    }

    private static Result load(Identifier id, String path, String resource, boolean cape) {
        NativeImage img = null;
        try {
            boolean custom = path != null && !path.isBlank();
            if (custom) {
                try (InputStream in = Files.newInputStream(Path.of(path.trim()))) {
                    img = NativeImage.read(in);
                }
            } else if (resource != null) {
                try (InputStream in = NHSkin.class.getResourceAsStream(resource)) {
                    img = NativeImage.read(in);
                }
            } else {
                return new Result(null, "none");
            }
            int w = img.getWidth(), h = img.getHeight();
            boolean ok = cape ? (w >= 64 && w == 2 * h) : (w >= 64 && w == h);
            if (!ok) {
                img.close();
                return new Result(null, "bad");
            }
            TextureManager tm = Minecraft.getInstance().getTextureManager();
            tm.register(id, new DynamicTexture(() -> id.toString(), img));
            return new Result(new ClientAsset.ResourceTexture(id, id), custom ? "file" : "default");
        } catch (Exception e) {
            if (img != null) img.close();
            return new Result(null, "err");
        }
    }
}
