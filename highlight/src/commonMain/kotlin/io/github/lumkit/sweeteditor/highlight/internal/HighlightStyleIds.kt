package io.github.lumkit.sweeteditor.highlight.internal

internal object HighlightStyleIds {
    const val KEYWORD = "keyword"
    const val STRING = "string"
    const val NUMBER = "number"
    const val COMMENT = "comment"
    const val CLASS = "class"
    const val METHOD = "method"
    const val VARIABLE = "variable"
    const val PUNCTUATION = "punctuation"
    const val ANNOTATION = "annotation"
    const val BUILTIN = "builtin"
    const val PREPROCESSOR = "preprocessor"
    const val MACRO = "macro"
    const val PROPERTY = "property"
    const val LIFETIME = "lifetime"
    const val SELECTOR = "selector"
    const val URL = "url"

    const val KEYWORD_ID = 1
    const val STRING_ID = 2
    const val NUMBER_ID = 3
    const val COMMENT_ID = 4
    const val CLASS_ID = 5
    const val METHOD_ID = 6
    const val VARIABLE_ID = 7
    const val PUNCTUATION_ID = 8
    const val ANNOTATION_ID = 9
    const val BUILTIN_ID = 10
    const val PREPROCESSOR_ID = 11
    const val MACRO_ID = 12
    const val PROPERTY_ID = 13
    const val LIFETIME_ID = 14
    const val SELECTOR_ID = 15
    const val URL_ID = 16
    const val RAINBOW_0 = 100
    const val RAINBOW_1 = 101
    const val RAINBOW_2 = 102
    const val RAINBOW_3 = 103
    const val RAINBOW_4 = 104
    const val RAINBOW_5 = 105
    const val BRACKET_UNMATCHED = 106
    const val RAINBOW_UNKNOWN_0 = 110
    const val RAINBOW_UNKNOWN_1 = 111
    const val RAINBOW_UNKNOWN_2 = 112
    const val RAINBOW_UNKNOWN_3 = 113
    const val RAINBOW_UNKNOWN_4 = 114
    const val RAINBOW_UNKNOWN_5 = 115
    const val DYNAMIC_START = 200

    const val BRACKET_UNMATCHED_NAME = "bracket_unmatched"

    val RAINBOW_NAMES: List<String> = listOf(
        "rainbow_0",
        "rainbow_1",
        "rainbow_2",
        "rainbow_3",
        "rainbow_4",
        "rainbow_5",
    )

    val RAINBOW_UNKNOWN_NAMES: List<String> = listOf(
        "rainbow_unknown_0",
        "rainbow_unknown_1",
        "rainbow_unknown_2",
        "rainbow_unknown_3",
        "rainbow_unknown_4",
        "rainbow_unknown_5",
    )

    val VOCABULARY: Map<String, Int> = linkedMapOf(
        KEYWORD to KEYWORD_ID,
        STRING to STRING_ID,
        NUMBER to NUMBER_ID,
        COMMENT to COMMENT_ID,
        CLASS to CLASS_ID,
        METHOD to METHOD_ID,
        VARIABLE to VARIABLE_ID,
        PUNCTUATION to PUNCTUATION_ID,
        ANNOTATION to ANNOTATION_ID,
        BUILTIN to BUILTIN_ID,
        PREPROCESSOR to PREPROCESSOR_ID,
        MACRO to MACRO_ID,
        PROPERTY to PROPERTY_ID,
        LIFETIME to LIFETIME_ID,
        SELECTOR to SELECTOR_ID,
        URL to URL_ID,
        RAINBOW_NAMES[0] to RAINBOW_0,
        RAINBOW_NAMES[1] to RAINBOW_1,
        RAINBOW_NAMES[2] to RAINBOW_2,
        RAINBOW_NAMES[3] to RAINBOW_3,
        RAINBOW_NAMES[4] to RAINBOW_4,
        RAINBOW_NAMES[5] to RAINBOW_5,
        BRACKET_UNMATCHED_NAME to BRACKET_UNMATCHED,
        RAINBOW_UNKNOWN_NAMES[0] to RAINBOW_UNKNOWN_0,
        RAINBOW_UNKNOWN_NAMES[1] to RAINBOW_UNKNOWN_1,
        RAINBOW_UNKNOWN_NAMES[2] to RAINBOW_UNKNOWN_2,
        RAINBOW_UNKNOWN_NAMES[3] to RAINBOW_UNKNOWN_3,
        RAINBOW_UNKNOWN_NAMES[4] to RAINBOW_UNKNOWN_4,
        RAINBOW_UNKNOWN_NAMES[5] to RAINBOW_UNKNOWN_5,
    )

    val NAME_BY_ID: Map<Int, String> = VOCABULARY.entries.associate { it.value to it.key }
}
