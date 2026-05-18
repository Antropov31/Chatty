package ru.brikster.chatty.chat.message.transform.decorations;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlayerDecorationsFormatterTest {

    private static final String SECTION = "§";

    private final PlayerDecorationsFormatter formatter = new PlayerDecorationsFormatter();

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    private static String miniMessage(Component component) {
        return MiniMessage.miniMessage().serialize(component);
    }

    @Test
    void stripsSectionCodesForPlayerWithoutPermission() {
        // A fresh mock denies every permission.
        CommandSender sender = mock(CommandSender.class);

        Component result = formatter.formatMessageWithDecorations(sender, "hello " + SECTION + "cworld");

        assertFalse(plain(result).contains(SECTION), "section sign must be stripped");
        assertEquals("hello cworld", miniMessage(result), "no color must be applied");
    }

    @Test
    void stripsSectionCodesEvenForPlayerWithDecorationPermission() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("chatty.decoration")).thenReturn(true);

        Component result = formatter.formatMessageWithDecorations(sender, SECTION + "cmessage");

        assertFalse(plain(result).contains(SECTION), "section sign must be stripped");
        assertEquals("cmessage", miniMessage(result), "section codes never act as colors");
    }

    @Test
    void keepsAmpersandCodesLiteralWithoutPermission() {
        CommandSender sender = mock(CommandSender.class);

        Component result = formatter.formatMessageWithDecorations(sender, "&cred text");

        assertEquals("&cred text", plain(result), "& codes stay literal without permission");
    }

    @Test
    void appliesAmpersandColorsWithPermission() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("chatty.decoration")).thenReturn(true);

        Component result = formatter.formatMessageWithDecorations(sender, "&cred text");

        assertTrue(miniMessage(result).contains("red"), "& codes are colored with permission");
    }

}
