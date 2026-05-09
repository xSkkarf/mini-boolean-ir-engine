package it.unipv.ir.similarity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds w-shingles (contiguous w-token sequences) from a token list
 * and hashes each shingle to a 64-bit long fingerprint.
 *
 * Shingles capture local word-order context. Two documents with many
 * shared shingles are likely near-duplicates or closely related.
 *
 * Default shingle width w=3 (trigram shingles). Smaller w detects
 * more similarity but with more false positives; larger w is more precise.
 *
 * Hashing uses a simple but fast polynomial rolling hash so that
 * identical shingle strings always map to the same fingerprint.
 */
public class ShingleHash {

    private static final int DEFAULT_W = 3;
    private static final long HASH_BASE  = 31L;
    private static final long HASH_MOD   = (1L << 61) - 1; // Mersenne prime

    private final int w;

    public ShingleHash() { this.w = DEFAULT_W; }
    public ShingleHash(int w) { this.w = w; }

    /**
     * Build the set of shingle fingerprints for a token list.
     * Each shingle is a w-token window; overlapping windows are all included.
     */
    public Set<Long> shingles(List<String> tokens) {
        Set<Long> result = new HashSet<>();
        if (tokens.size() < w) {
            // Document too short for w-shingles: hash the whole thing
            if (!tokens.isEmpty()) result.add(hashTokens(tokens, 0, tokens.size()));
            return result;
        }
        for (int i = 0; i <= tokens.size() - w; i++) {
            result.add(hashTokens(tokens, i, i + w));
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private long hashTokens(List<String> tokens, int from, int to) {
        long h = 0;
        for (int i = from; i < to; i++) {
            h = addMod(mulMod(h, HASH_BASE), stringHash(tokens.get(i)));
        }
        return h;
    }

    private long stringHash(String s) {
        long h = 0;
        for (char c : s.toCharArray()) {
            h = addMod(mulMod(h, HASH_BASE), c);
        }
        return h;
    }

    // Modular arithmetic helpers for Mersenne prime
    private long mulMod(long a, long b) {
        return Math.floorMod(a * b, HASH_MOD);
    }

    private long addMod(long a, long b) {
        long r = a + b;
        return r >= HASH_MOD ? r - HASH_MOD : r;
    }

    public int getW() { return w; }
}
