package it.unipv.ir.similarity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds and stores shingle-based fingerprints for all documents.
 *
 * A document fingerprint is the set of hashed w-shingles of its tokens.
 * Similarity between two documents is computed as Jaccard similarity
 * of their fingerprint sets.
 *
 * This class maintains an in-memory map of docId → fingerprint.
 * For 10k documents this is easily tractable.
 */
public class FingerprintBuilder {

    private final ShingleHash                  shingler;
    private final Map<Integer, Set<Long>>      fingerprints = new HashMap<>();

    public FingerprintBuilder() {
        this.shingler = new ShingleHash();
    }

    public FingerprintBuilder(int shingleWidth) {
        this.shingler = new ShingleHash(shingleWidth);
    }

    // -------------------------------------------------------------------------
    // Building
    // -------------------------------------------------------------------------

    /** Compute and store the fingerprint for a document. */
    public void addDocument(int docId, List<String> tokens) {
        fingerprints.put(docId, shingler.shingles(tokens));
    }

    // -------------------------------------------------------------------------
    // Lookup
    // -------------------------------------------------------------------------

    public Set<Long> getFingerprint(int docId) {
        return fingerprints.get(docId);
    }

    public boolean hasFingerprint(int docId) {
        return fingerprints.containsKey(docId);
    }

    public Map<Integer, Set<Long>> getAllFingerprints() {
        return fingerprints;
    }

    public int size() { return fingerprints.size(); }
}
