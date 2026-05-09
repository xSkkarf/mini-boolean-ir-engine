package it.unipv.ir.ranking;

import it.unipv.ir.datastructures.BinaryHeap;

import java.util.List;

/**
 * Extracts the top-K highest-scoring documents from a list of ScoredDocuments
 * using a min-heap of size K.
 *
 * Algorithm:
 *   - Maintain a min-heap of size K.
 *   - For each document, if its score > heap.min(), replace heap.min().
 *   - After processing all documents, drain the heap in reverse order
 *     to get descending score order.
 *
 * Time: O(n log K) — much better than O(n log n) full sort when K << n.
 * Space: O(K)
 */
public class TopKHeap {

    private final int K;

    public TopKHeap(int k) {
        this.K = k;
    }

    /**
     * Return the top-K documents from an unordered list of ScoredDocuments,
     * sorted by descending score.
     */
    public List<ScoredDocument> topK(List<ScoredDocument> candidates) {
        if (candidates.isEmpty()) return List.of();

        // Min-heap: the minimum score is always at the root
        BinaryHeap<ScoredDocument> heap = new BinaryHeap<>(true);

        for (ScoredDocument doc : candidates) {
            if (heap.size() < K) {
                heap.insert(doc);
            } else if (doc.getScore() > heap.peek().getScore()) {
                heap.extract();   // remove current minimum
                heap.insert(doc);
            }
        }

        // Drain in ascending order, then reverse
        List<ScoredDocument> ascending = heap.drainSorted();
        java.util.Collections.reverse(ascending);
        return ascending;
    }

    public int getK() { return K; }
}
