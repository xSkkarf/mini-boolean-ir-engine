package it.unipv.ir.index;

import it.unipv.ir.datastructures.Trie;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * K-gram index: maps every k-gram of every vocabulary term to the set
 * of terms that contain it.
 *
 * Used for:
 *   1. Wildcard query resolution — "comp*ter" → find terms sharing
 *      k-grams with the wildcard pattern.
 *   2. Spelling correction candidate generation — terms sharing many
 *      k-grams with the misspelled word are likely candidates.
 *
 * Default k=2 (bigrams) and k=3 (trigrams) are both supported.
 * We index bigrams by default and also support 3-grams.
 *
 * Each term is padded with '$' sentinels: "hello" → "$hello$"
 * before extracting grams, so boundary grams ($h, he, el, ll, lo, o$)
 * are distinguishable from mid-word grams.
 */
public class KGramIndex {

    private final Trie bigrams;   // k=2
    private final Trie trigrams;  // k=3
    private final Set<String> vocabulary;

    public KGramIndex() {
        this.bigrams    = new Trie();
        this.trigrams   = new Trie();
        this.vocabulary = new HashSet<>();
    }

    // -------------------------------------------------------------------------
    // Building
    // -------------------------------------------------------------------------

    /** Add a vocabulary term to the k-gram index. */
    public void addTerm(String term) {
        if (vocabulary.contains(term)) return;
        vocabulary.add(term);

        String padded = "$" + term + "$";
        // Bigrams
        for (int i = 0; i < padded.length() - 1; i++) {
            String gram = padded.substring(i, i + 2);
            bigrams.insert(gram, term);
        }
        // Trigrams
        for (int i = 0; i < padded.length() - 2; i++) {
            String gram = padded.substring(i, i + 3);
            trigrams.insert(gram, term);
        }
    }

    /** Add all terms from a vocabulary collection. */
    public void addAllTerms(Iterable<String> terms) {
        for (String t : terms) addTerm(t);
    }

    // -------------------------------------------------------------------------
    // Lookup
    // -------------------------------------------------------------------------

    /** Return all vocabulary terms containing this bigram. */
    public Set<String> lookupBigram(String gram) {
        return bigrams.lookup(gram);
    }

    /** Return all vocabulary terms containing this trigram. */
    public Set<String> lookupTrigram(String gram) {
        return trigrams.lookup(gram);
    }

    /**
     * Given a wildcard pattern (single '*'), return the set of vocabulary
     * terms that are consistent with it based on k-gram overlap.
     *
     * Strategy:
     *   1. Split pattern at '*' into prefix and suffix parts.
     *   2. Extract k-grams from each part (with boundary sentinels).
     *   3. Start with candidates from the first k-gram, then intersect
     *      with candidates of each subsequent k-gram.
     *   4. Post-filter: check each candidate actually matches the pattern.
     */
    public Set<String> resolveWildcard(String pattern) {
        List<String> grams = extractGramsFromPattern(pattern);
        if (grams.isEmpty()) return new HashSet<>(vocabulary);

        Set<String> candidates = null;
        for (String gram : grams) {
            Set<String> matches = trigrams.lookup(gram);
            if (matches.isEmpty()) matches = bigrams.lookup(gram);
            if (candidates == null) {
                candidates = new HashSet<>(matches);
            } else {
                candidates.retainAll(matches);
            }
        }

        if (candidates == null) return new HashSet<>();

        // Post-filter with actual pattern match
        Set<String> result = new HashSet<>();
        String regex = "^" + pattern.replace("*", ".*") + "$";
        for (String c : candidates) {
            if (c.matches(regex)) result.add(c);
        }
        return result;
    }

    /**
     * Return vocabulary terms with at least minOverlap k-grams in common
     * with the given word. Used for spelling correction candidate generation.
     */
    public Set<String> getCandidatesByOverlap(String word, int minOverlap) {
        String padded = "$" + word + "$";
        List<String> wordGrams = new ArrayList<>();
        for (int i = 0; i < padded.length() - 2; i++) {
            wordGrams.add(padded.substring(i, i + 3));
        }

        // Count how many grams each candidate shares
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        for (String gram : wordGrams) {
            for (String candidate : trigrams.lookup(gram)) {
                counts.merge(candidate, 1, Integer::sum);
            }
        }

        Set<String> result = new HashSet<>();
        for (java.util.Map.Entry<String, Integer> e : counts.entrySet()) {
            if (e.getValue() >= minOverlap) result.add(e.getKey());
        }
        return result;
    }

    public Set<String> getVocabulary() { return vocabulary; }
    public int size()                  { return vocabulary.size(); }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Extract k-grams from a wildcard pattern.
     * The '*' is treated as a boundary: parts are padded and grams extracted.
     * Example: "hel*o" → pad each part → "$hel", "o$" → extract grams.
     */
    private List<String> extractGramsFromPattern(String pattern) {
        List<String> grams = new ArrayList<>();
        String[] parts = pattern.split("\\*", -1);

        for (int p = 0; p < parts.length; p++) {
            String part = parts[p];
            if (part.isEmpty()) continue;

            // Add sentinels only at actual boundaries of the full term
            String padded = (p == 0 ? "$" : "") + part +
                            (p == parts.length - 1 ? "$" : "");
            // Extract trigrams from this padded part
            for (int i = 0; i <= padded.length() - 3; i++) {
                grams.add(padded.substring(i, i + 3));
            }
            // Fallback to bigrams if part is too short for trigrams
            if (padded.length() == 2) {
                grams.add(padded);
            }
        }
        return grams;
    }
}
