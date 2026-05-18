package ru.brikster.chatty.config.file;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the bundled lang/*.yml files against drifting away from
 * {@link MessagesConfig}: every message key must be present, with no
 * unknown or empty entries.
 */
class LanguageFilesTest {

    @ParameterizedTest
    @ValueSource(strings = {"ru-RU", "de-DE", "es-ES", "zh-CN"})
    void bundledLanguageFileMatchesMessageKeys(String language) throws Exception {
        Set<String> expected = messageKeys();

        Map<String, Object> data;
        try (InputStream in = getClass().getResourceAsStream("/lang/" + language + ".yml")) {
            assertNotNull(in, "bundled lang/" + language + ".yml is missing");
            data = new Yaml().load(in);
        }
        Set<String> actual = new TreeSet<>(data.keySet());

        Set<String> missing = expected.stream()
                .filter(key -> !actual.contains(key))
                .collect(Collectors.toCollection(TreeSet::new));
        Set<String> unknown = actual.stream()
                .filter(key -> !expected.contains(key))
                .collect(Collectors.toCollection(TreeSet::new));

        assertTrue(missing.isEmpty(), language + " is missing message keys: " + missing);
        assertTrue(unknown.isEmpty(), language + " has unknown keys: " + unknown);

        for (String key : actual) {
            assertFalse(String.valueOf(data.get(key)).isBlank(),
                    language + " has an empty value for \"" + key + "\"");
        }
    }

    /** The hyphen-case keys okaeri derives from MessagesConfig's fields. */
    private static Set<String> messageKeys() {
        Set<String> keys = new TreeSet<>();
        for (Field field : MessagesConfig.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            keys.add(toHyphenCase(field.getName()));
        }
        return keys;
    }

    private static String toHyphenCase(String camelCase) {
        StringBuilder builder = new StringBuilder();
        for (char c : camelCase.toCharArray()) {
            if (Character.isUpperCase(c)) {
                builder.append('-').append(Character.toLowerCase(c));
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

}
