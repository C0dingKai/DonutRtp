package Kai.donutRtp.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.List;

public class ColorUtil {

    public static Component parse(String input) {
        return MiniMessage.miniMessage().deserialize(input).decoration(TextDecoration.ITALIC, false);
    }

    public static List<Component> parseList(List<String> input) {
        return input.stream()
                .map(ColorUtil::parse)
                .toList();
    }

    public static Component empty() {
        return Component.empty();
    }
}