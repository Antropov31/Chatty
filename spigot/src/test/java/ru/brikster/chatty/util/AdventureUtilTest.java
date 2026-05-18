package ru.brikster.chatty.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdventureUtilTest {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{x}");

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    @Test
    void replacesPlaceholderWithComponent() {
        Component result = AdventureUtil.replaceWithEndingSpace(
                Component.text("hello {x} world"),
                PLACEHOLDER,
                matched -> Component.text("REPLACED "),
                matched -> "REPLACED");
        assertEquals("hello REPLACED world", plain(result));
    }

    @Test
    void keepsTextWhenReplacementFunctionReturnsNull() {
        Component result = AdventureUtil.replaceWithEndingSpace(
                Component.text("hello {x} world"),
                PLACEHOLDER,
                matched -> null,
                matched -> null);
        assertEquals("hello {x} world", plain(result));
    }

    @Test
    void leavesTextWithoutMatchesUnchanged() {
        Component result = AdventureUtil.replaceWithEndingSpace(
                Component.text("nothing to replace here"),
                PLACEHOLDER,
                matched -> Component.text("REPLACED "),
                matched -> "REPLACED");
        assertEquals("nothing to replace here", plain(result));
    }

}
