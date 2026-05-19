package ru.brikster.chatty.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

    private static ClickEvent firstClick(Component component) {
        if (component.clickEvent() != null) {
            return component.clickEvent();
        }
        for (Component child : component.children()) {
            ClickEvent click = firstClick(child);
            if (click != null) {
                return click;
            }
        }
        return null;
    }

    @Test
    void preservesClickEventOfReplacementComponent() {
        // Regression: a mention component carries a suggest_command click event;
        // substituting it into the {message} placeholder must not drop it.
        Component mention = Component.text("")
                .append(Component.text("@Steve", NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.suggestCommand("/msg Steve ")))
                .append(Component.text(" hi there"));

        Component result = AdventureUtil.replaceWithEndingSpace(
                Component.text("[chat] {x}"),
                PLACEHOLDER,
                matched -> mention.append(Component.text(" ")),
                matched -> "STEVE");

        ClickEvent click = firstClick(result);
        assertNotNull(click, "click event was dropped while substituting the component");
        assertEquals(ClickEvent.Action.SUGGEST_COMMAND, click.action());
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
