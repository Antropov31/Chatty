package ru.brikster.chatty.convert.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.junit.jupiter.api.Test;
import ru.brikster.chatty.convert.component.InternalMiniMessageStringConverter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LegacyToMiniMessageConverterTest {

    /** The decoration-reset tags every color code is prefixed with. */
    private static final String RESET = "<!bold><!italic><!strikethrough><!obfuscated><!underlined>";
    private static final char SECTION = '§';

    private final LegacyToMiniMessageConverter converter = new LegacyToMiniMessageConverter();

    @Test
    void convertsAmpersandColorCode() {
        assertEquals(RESET + "<color:green>Hello", converter.convert("&aHello"));
    }

    @Test
    void convertsSectionColorCode() {
        assertEquals(RESET + "<color:green>Hello", converter.convert(SECTION + "aHello"));
    }

    @Test
    void convertsDecorationCode() {
        assertEquals("<bold>Bold", converter.convert("&lBold"));
    }

    @Test
    void convertsHexColorCode() {
        assertEquals(RESET + "<color:#abcdef>Text", converter.convert("&#abcdefText"));
    }

    @Test
    void leavesPlainTextUntouched() {
        assertEquals("just plain text", converter.convert("just plain text"));
    }

    @Test
    void convertSectionCodesIgnoresAmpersandCodes() {
        // & codes are gated behind the chatty.decoration permission and must
        // not be converted when sanitizing foreign text.
        assertEquals("&aHello", converter.convertSectionCodes("&aHello"));
    }

    @Test
    void convertSectionCodesConvertsSectionColorCode() {
        assertEquals(RESET + "<color:green>Hello", converter.convertSectionCodes(SECTION + "aHello"));
    }

    @Test
    void convertSectionCodesConvertsResetCode() {
        // Regression for #364: §r injected by other plugins must not reach the
        // strict MiniMessage deserializer as a raw legacy code.
        assertEquals(RESET + "<color:white>[/menu]", converter.convertSectionCodes(SECTION + "r[/menu]"));
    }

    @Test
    void convertSectionCodesIsCaseInsensitive() {
        assertEquals(RESET + "<color:green>Hi", converter.convertSectionCodes(SECTION + "AHi"));
    }

    @Test
    void convertSectionCodesLeavesPlainTextUntouched() {
        assertEquals("plain text", converter.convertSectionCodes("plain text"));
    }

    @Test
    void convertSectionCodesMakesBotMessageSafeForStrictMiniMessage() {
        // Issue #370: a chat bot sends a message with legacy § codes around a
        // mention; it must not crash the strict MiniMessage parser.
        String safe = converter.convertSectionCodes(SECTION + "c@Juz1k_" + SECTION + "r");
        assertFalse(safe.contains(String.valueOf(SECTION)), "no section sign may survive");
        assertDoesNotThrow(() -> MiniMessage.miniMessage().deserialize(safe));
    }

    @Test
    void convertSectionCodesStripsStraySectionSigns() {
        // § that is not part of a recognised code must still be removed.
        for (String input : new String[] {SECTION + "z", "text" + SECTION, "a" + SECTION + "!b", "" + SECTION}) {
            String safe = converter.convertSectionCodes(input);
            assertFalse(safe.contains(String.valueOf(SECTION)), "stray section sign left in: " + input);
            assertDoesNotThrow(() -> MiniMessage.miniMessage().deserialize(safe));
        }
    }

    @Test
    void messageWithLegacyCodesSurvivesTheConstructPipeline() {
        // Reproduces issue #370 end to end: a message component carrying legacy
        // § codes (e.g. from a chat bot) goes through componentToString and the
        // section-code conversion the same way ComponentFromContextConstructorImpl
        // does, and must not crash the strict MiniMessage deserializer.
        InternalMiniMessageStringConverter stringConverter = new InternalMiniMessageStringConverter();
        Component botMessage = Component.text(SECTION + "c@Juz1k_" + SECTION + "r");
        String asString = stringConverter.componentToString(botMessage);
        String safe = converter.convertSectionCodes(asString);
        assertDoesNotThrow(() -> MiniMessage.miniMessage().deserialize(safe));
    }

}
