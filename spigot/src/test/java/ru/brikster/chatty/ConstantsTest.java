package ru.brikster.chatty;

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConstantsTest {

    @Test
    void matchesLowercaseReplacementKey() {
        assertTrue(Constants.REPLACEMENTS_PATTERN.matcher("{r_player_info}").matches());
    }

    @Test
    void matchesUppercaseReplacementKey() {
        // Regression for #363: uppercase keys were silently shown as literal text.
        assertTrue(Constants.REPLACEMENTS_PATTERN.matcher("{r_PlayerInfo}").matches());
        assertTrue(Constants.REPLACEMENTS_PATTERN.matcher("{R_test}").matches());
    }

    @Test
    void findsReplacementKeyInsideText() {
        Matcher matcher = Constants.REPLACEMENTS_PATTERN.matcher("prefix {r_info} suffix");
        assertTrue(matcher.find());
        assertEquals("{r_info}", matcher.group());
    }

    @Test
    void doesNotMatchUnrelatedPlaceholders() {
        assertFalse(Constants.REPLACEMENTS_PATTERN.matcher("{player}").matches());
        assertFalse(Constants.REPLACEMENTS_PATTERN.matcher("{x_test}").matches());
    }

}
