package it.unipv.ir.preprocessing;

import java.util.ArrayList;
import java.util.List;

/**
 * Normalises a list of tokens by:
 *   1. Applying Unicode NFC decomposition (handles accented characters).
 *   2. Stripping combining diacritical marks (é → e, ü → u, etc.).
 *   3. Keeping only ASCII alphanumeric characters.
 *
 * This is called after tokenisation, before stop-word filtering
 * and stemming.
 */
public class LinguisticNormalizer {

    // Regex matching any Unicode combining character (diacritics etc.)
    private static final java.util.regex.Pattern DIACRITICS =
        java.util.regex.Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    public List<String> normalize(List<String> tokens) {
        List<String> result = new ArrayList<>(tokens.size());
        for (String t : tokens) {
            String norm = normalizeToken(t);
            if (!norm.isEmpty()) result.add(norm);
        }
        return result;
    }

    public String normalizeToken(String token) {
        if (token == null) return "";
        // NFC decomposition then strip diacritics
        String decomposed = java.text.Normalizer.normalize(token,
            java.text.Normalizer.Form.NFD);
        return DIACRITICS.matcher(decomposed)
            .replaceAll("")
            .toLowerCase();
    }
}
