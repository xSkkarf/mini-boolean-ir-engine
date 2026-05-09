package it.unipv.ir.ranking;

/**
 * A document paired with its relevance score.
 * Comparable by score descending (highest score = highest rank).
 */
public class ScoredDocument implements Comparable<ScoredDocument> {

    private final int    docId;
    private final double score;

    public ScoredDocument(int docId, double score) {
        this.docId = docId;
        this.score = score;
    }

    public int    getDocId() { return docId; }
    public double getScore() { return score; }

    /** Natural order: higher score = smaller (for min-heap usage). */
    @Override
    public int compareTo(ScoredDocument other) {
        return Double.compare(this.score, other.score);
    }

    @Override
    public String toString() {
        return "ScoredDocument{docId=" + docId
            + ", score=" + String.format("%.4f", score) + "}";
    }
}
