package it.unipv.ir.compression;

import java.util.ArrayList;
import java.util.List;

/**
 * Front-coding dictionary compression.
 *
 * Sorted vocabulary terms share long common prefixes. Front coding exploits
 * this by storing only the differing suffix of each term relative to the
 * previous one, along with the length of the shared prefix.
 *
 * Format for each term (after the first in a block):
 *   [prefix_len (1 byte)] [suffix_bytes] [0x00 terminator]
 *
 * Terms are grouped into blocks of BLOCK_SIZE. The first term in each
 * block is stored verbatim (the "front"). This allows binary search on
 * block fronts without decompressing the entire dictionary.
 *
 * Example (block of 4):
 *   automata    → stored as-is
 *   automate    → 7|e      (shared 7 chars "automat", suffix "e")
 *   automatic   → 7|ic     (shared 7 chars, suffix "ic")
 *   automation  → 7|ion    (shared 7 chars, suffix "ion")
 *
 * Typical compression: 60–75% size reduction on English vocabulary.
 *
 * Reference: Manning et al., §5.2.2.
 */
public class FrontCodingDictionary {

    private static final int BLOCK_SIZE = 8;

    private final List<Block> blocks = new ArrayList<>();
    private int totalTerms = 0;

    // -------------------------------------------------------------------------
    // Building
    // -------------------------------------------------------------------------

    /**
     * Build the compressed dictionary from a sorted list of terms.
     * The input MUST be sorted alphabetically.
     */
    public void build(List<String> sortedTerms) {
        blocks.clear();
        totalTerms = sortedTerms.size();

        for (int i = 0; i < sortedTerms.size(); i += BLOCK_SIZE) {
            int end = Math.min(i + BLOCK_SIZE, sortedTerms.size());
            List<String> blockTerms = sortedTerms.subList(i, end);
            blocks.add(compressBlock(blockTerms));
        }
    }

    // -------------------------------------------------------------------------
    // Lookup
    // -------------------------------------------------------------------------

    /**
     * Check whether a term exists in the compressed dictionary.
     * O(B * average_term_length) where B = BLOCK_SIZE.
     */
    public boolean contains(String term) {
        // Binary search over block fronts
        int lo = 0, hi = blocks.size() - 1;
        while (lo <= hi) {
            int mid = (lo + hi) / 2;
            int cmp = term.compareTo(blocks.get(mid).front);
            if (cmp == 0)      return true;
            else if (cmp < 0)  hi = mid - 1;
            else               lo = mid + 1;
        }
        // Check the block at lo-1 (term may be inside a non-front entry)
        int blockIdx = Math.max(0, lo - 1);
        if (blockIdx < blocks.size()) {
            List<String> decoded = decodeBlock(blocks.get(blockIdx));
            return decoded.contains(term);
        }
        return false;
    }

    /**
     * Decode and return all terms in the dictionary.
     * Used for iterating the vocabulary (e.g. when building k-gram index).
     */
    public List<String> decodeAll() {
        List<String> result = new ArrayList<>(totalTerms);
        for (Block block : blocks) {
            result.addAll(decodeBlock(block));
        }
        return result;
    }

    public int size()        { return totalTerms; }
    public int blockCount()  { return blocks.size(); }

    // -------------------------------------------------------------------------
    // Private compression helpers
    // -------------------------------------------------------------------------

    private Block compressBlock(List<String> terms) {
        if (terms.isEmpty()) throw new IllegalArgumentException("Empty block");

        String front = terms.get(0);
        // Each entry after the first is: (prefixLen, suffix)
        List<int[]> prefixLens = new ArrayList<>();
        List<String> suffixes  = new ArrayList<>();

        for (int i = 1; i < terms.size(); i++) {
            String prev = terms.get(i - 1);
            String curr = terms.get(i);
            int shared = sharedPrefixLength(prev, curr);
            prefixLens.add(new int[]{shared});
            suffixes.add(curr.substring(shared));
        }

        return new Block(front, prefixLens, suffixes);
    }

    private List<String> decodeBlock(Block block) {
        List<String> result = new ArrayList<>();
        result.add(block.front);
        String prev = block.front;
        for (int i = 0; i < block.suffixes.size(); i++) {
            int shared = block.prefixLens.get(i)[0];
            String term = prev.substring(0, shared) + block.suffixes.get(i);
            result.add(term);
            prev = term;
        }
        return result;
    }

    private int sharedPrefixLength(String a, String b) {
        int max = Math.min(a.length(), b.length());
        int i = 0;
        while (i < max && a.charAt(i) == b.charAt(i)) i++;
        return i;
    }

    // -------------------------------------------------------------------------
    // Block structure
    // -------------------------------------------------------------------------

    private static class Block {
        final String       front;
        final List<int[]>  prefixLens;
        final List<String> suffixes;

        Block(String front, List<int[]> prefixLens, List<String> suffixes) {
            this.front      = front;
            this.prefixLens = prefixLens;
            this.suffixes   = suffixes;
        }
    }
}
