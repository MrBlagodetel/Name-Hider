package com.namehider;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

public final class NHFiles {
    private NHFiles() {}

    /** Окно выбора PNG-файла. Возвращает путь или null. */
    public static String pickPng(String title) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = stack.mallocPointer(1);
            filters.put(stack.UTF8("*.png")).flip();
            return TinyFileDialogs.tinyfd_openFileDialog(title, "", filters, "PNG (*.png)", false);
        } catch (Throwable t) {
            return null;
        }
    }
}
