package it.unipv.ir.index;

import it.unipv.ir.datastructures.Trie;

import java.util.HashSet;
import java.util.Set;

/**
 * Permuterm index for wildcard query resolution.
 *
 * Each vocabulary term t is rotated through all cyclic permutations
 * of t$ (where $ is a sentinel), and each rotation is stored in a Trie
 * pointing back to t.
 *
 * Example: "hello$" produces rotations:
 *   hello$ → hello
 *   ello$h → hello
 *   llo$he → hello
 *   lo$hel → hello
 *   o$hell → hello
 *   $hello → hello
 *
 * Wildcard resolution:
 *   Pattern "hel*o" → rotate so '*' is at end → "o$hel*" → prefix "o$hel"
 *   Prefix lookup in the Trie returns all terms matching "hel*o".
 *
 *   Pattern "hel*"  → rotate → "hel*$" → prefix "hel"... wait,
 *   actually for "hel*" we want terms starting with "hel":
 *   append $ → "hel*$" → the part after * is empty → prefix "$hel"
 *   (since we rotate so * comes last, the prefix before * appears after $).
 *
 * All four wildcard types are handled:
 *   X*   → prefix lookup on $X
 *   *X   → prefix lookup on X$
 *   X*Y  → prefix lookup on Y$X
 *   *    → return entire vocabulary
 */
public class PermutermIndex {

    private final Trie trie = new Trie();
    private final Set<String> vocabulary = new HashSet<>();

    // -------------------------------------------------------------------------
    // Building
    // -------------------------------------------------------------------------

    public void addTerm(String term) {
        if (vocabulary.contains(term)) return;
        vocabulary.add(term);

        String t = term + "$";
        // Insert all rotations
        for (int i = 0; i < t.length(); i++) {
            String rotation = t.substring(i) + t.substring(0, i);
            trie.insert(rotation, term);
        }
    }

    public void addAllTerms(Iterable<String> terms) {
        for (String t : terms) addTerm(t);
    }

    // -------------------------------------------------------------------------
    // Wildcard resolution
    // -------------------------------------------------------------------------

    /**
     * Resolve a wildcard pattern containing exactly one '*'.
     * Returns the set of vocabulary terms matching the pattern.
     */
    public Set<String> resolve(String pattern) {
        if (!pattern.contains("*")) {
            // Exact match — just check vocabulary
            return vocabulary.contains(pattern)
                ? Set.of(pattern) : Set.of();
        }

        if (pattern.equals("*")) return new HashSet<>(vocabulary);

        int star = pattern.indexOf('*');
        String prefix = pattern.substring(0, star);      // part before *
        String suffix = pattern.substring(star + 1);     // part after *

        // Rotate so * is at the end: suffix + "$" + prefix
        String rotated = suffix + "$" + prefix;

        Set<String> candidates = trie.prefixLookup(rotated);

        // Post-filter with regex for correctness
        Set<String> result = new HashSet<>();
        String regex = "^" + pattern.replace("*", ".*") + "$";
        for (String c : candidates) {
            if (c.matches(regex)) result.add(c);
        }
        return result;
    }

    public Set<String> getVocabulary() { return vocabulary; }
    public int size()                  { return vocabulary.size(); }
}
