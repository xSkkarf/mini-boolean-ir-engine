package it.unipv.ir.similarity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Computes document similarity using Jaccard coefficient over shingle sets.
 *
 * Jaccard(A, B) = |A ∩ B| / |A ∪ B|
 *
 * Returns a value in [0, 1] where 1 = identical shingle sets (near-duplicate)
 * and 0 = no shared shingles (completely dissimilar).
 *
 * Typical use: find near-duplicate documents in the corpus, or find
 * documents similar to a given document.
 *
 * Threshold recommendation: Jaccard >= 0.5 → near-duplicate candidate.
 */
public class SimilarityChecker {

    private static final double DUPLICATE_THRESHOLD = 0.5;

    private final FingerprintBuilder builder;

    public SimilarityChecker(FingerprintBuilder builder) {
        this.builder = builder;
    }

    // -------------------------------------------------------------------------
    // Pairwise similarity
    // -------------------------------------------------------------------------

    /**
     * Compute Jaccard similarity between two documents.
     * Returns 0.0 if either document has no fingerprint.
     */
    public double jaccard(int docId1, int docId2) {
        Set<Long> fp1 = builder.getFingerprint(docId1);
        Set<Long> fp2 = builder.getFingerprint(docId2);
        if (fp1 == null || fp2 == null || fp1.isEmpty() || fp2.isEmpty())
            return 0.0;
        return jaccardSets(fp1, fp2);
    }

    /**
     * Find all documents with Jaccard similarity >= threshold to docId.
     * Returns a list of (otherDocId, similarity) pairs, sorted descending.
     */
    public List<SimilarDoc> findSimilar(int docId, double threshold) {
        Set<Long> fp = builder.getFingerprint(docId);
        if (fp == null) return List.of();

        List<SimilarDoc> result = new ArrayList<>();
        for (Map.Entry<Integer, Set<Long>> e :
                builder.getAllFingerprints().entrySet()) {
            if (e.getKey() == docId) continue;
            double sim = jaccardSets(fp, e.getValue());
            if (sim >= threshold) {
                result.add(new SimilarDoc(e.getKey(), sim));
            }
        }
        result.sort((a, b) -> Double.compare(b.similarity, a.similarity));
        return result;
    }

    /**
     * Convenience: find near-duplicates (Jaccard >= DUPLICATE_THRESHOLD).
     */
    public List<SimilarDoc> findNearDuplicates(int docId) {
        return findSimilar(docId, DUPLICATE_THRESHOLD);
    }

    // -------------------------------------------------------------------------
    // Core Jaccard computation
    // -------------------------------------------------------------------------

    private double jaccardSets(Set<Long> a, Set<Long> b) {
        // Compute intersection size efficiently using the smaller set
        Set<Long> smaller = a.size() <= b.size() ? a : b;
        Set<Long> larger  = a.size() <= b.size() ? b : a;

        int intersection = 0;
        for (long x : smaller) {
            if (larger.contains(x)) intersection++;
        }
        int union = a.size() + b.size() - intersection;
        return union == 0 ? 0.0 : (double) intersection / union;
    }

    // -------------------------------------------------------------------------
    // Value object
    // -------------------------------------------------------------------------

    public static class SimilarDoc {
        public final int    docId;
        public final double similarity;

        SimilarDoc(int docId, double similarity) {
            this.docId      = docId;
            this.similarity = similarity;
        }

        @Override
        public String toString() {
            return String.format("SimilarDoc{docId=%d, jaccard=%.3f}",
                docId, similarity);
        }
    }
}
