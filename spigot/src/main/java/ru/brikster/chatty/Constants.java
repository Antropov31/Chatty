package ru.brikster.chatty;

import lombok.experimental.UtilityClass;

import java.util.regex.Pattern;

@UtilityClass
public class Constants {

    public Pattern REPLACEMENTS_PATTERN = Pattern.compile("\\{[rR]_[a-zA-Z0-9_]{1,24}}");

}
