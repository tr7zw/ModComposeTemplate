package dev.tr7zw.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

/**
 * Utility class to get Components, for better bridging between 1.19+ and pre 1.19
 * 
 * @author tr7zw
 *
 */
public class ComponentProvider {

    public static TextComponent literal(String string) {
        return new TextComponent(string);
    }

    public static TranslatableComponent translatable(String string) {
        return new TranslatableComponent(string);
    }

    public static TranslatableComponent translatable(String string, Object... objects) {
        return new TranslatableComponent(string, objects);
    }

    public static Component empty() {
        return TextComponent.EMPTY;
    }

}