package ru.brikster.chatty.convert.message;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

}
