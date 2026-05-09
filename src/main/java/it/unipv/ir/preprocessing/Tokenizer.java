package it.unipv.ir.preprocessing;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a text string into a list of raw tokens.
 *
 * Strategy:
 *   1. Lowercase everything.
 *   2. Split on any non-alphanumeric character (including hyphens,
 *      apostrophes, etc.) — this is the standard IR tokenisation.
 *   3. Discard tokens that are empty or purely numeric
 *      (numbers carry little retrieval value in this corpus).
 *   4. Discard tokens shorter than 2 characters.
 *
 * Positional offsets are preserved: the returned list is in the
 * same order as the tokens appeared in the text, so position i in
 * the list corresponds to token position i in the document.
 */
public class Tokenizer {

    // Regex: split on anything that is not a letter or digit
    private static final String SPLIT_PATTERN = "[^a-z0-9]+";

    /**
     * Tokenise text. Returns tokens in document order; duplicates retained.
     */
    public List<String> tokenize(String text) {
        if (text == null || text.isEmpty()) return List.of();

        String lower = text.toLowerCase();
        String[] parts = lower.split(SPLIT_PATTERN);

        List<String> tokens = new ArrayList<>(parts.length);
        for (String token : parts) {
            if (token.length() >= 2 && !isNumeric(token)) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    /** Quick numeric check — avoids regex overhead for every token. */
    private boolean isNumeric(String s) {
        for (char c : s.toCharArray()) {
            if (!Character.isDigit(c)) return false;
        }
        return true;
    }
}
