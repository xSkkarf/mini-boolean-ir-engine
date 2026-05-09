package it.unipv.ir.ranking;

import it.unipv.ir.datastructures.PostingList;
import it.unipv.ir.datastructures.PostingNode;
import it.unipv.ir.index.InvertedIndex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Okapi BM25 scoring.
 *
 * BM25 formula for term t in document d:
 *
 *   score(d, Q) = sum_t [ IDF(t) * (tf(t,d) * (k1+1)) /
 *                         (tf(t,d) + k1 * (1 - b + b * |d|/avgdl)) ]
 *
 *   IDF(t) = log( (N - df + 0.5) / (df + 0.5) + 1 )   (Robertson IDF)
 *
 * Parameters:
 *   k1 = 1.5  (term frequency saturation — standard value)
 *   b  = 0.75 (document length normalisation — standard value)
 *
 * BM25 is strictly better than TF-IDF for most IR tasks; both are
 * provided here for comparison in the project paper.
 */
public class BM25Scorer {

    // Standard BM25 hyper-parameters
    private static final double K1 = 1.5;
    private static final double B  = 0.75;

    private final InvertedIndex index;
    private final double        avgDocLength;

    public BM25Scorer(InvertedIndex index) {
        this.index        = index;
        this.avgDocLength = index.avgDocLength();
    }

    /**
     * Score all documents containing at least one query term using BM25.
     *
     * @param queryTerms preprocessed, stemmed query terms
     * @return list of ScoredDocument, unsorted
     */
    public List<ScoredDocument> score(List<String> queryTerms) {
        int N = index.getDocCount();
        if (N == 0 || avgDocLength == 0) return List.of();

        Map<Integer, Double> accum = new HashMap<>();

        for (String term : queryTerms) {
            PostingList pl = index.getPostingList(term);
            if (pl == null) continue;

            int df = pl.getSize();
            if (df == 0) continue;

            // Robertson IDF
            double idf = Math.log(
                (N - df + 0.5) / (df + 0.5) + 1.0);

            PostingNode node = pl.getHead();
            while (node != null) {
                double dl  = index.getDocLength(node.docId);
                double tf  = node.tf;
                double norm = K1 * (1.0 - B + B * dl / avgDocLength);
                double tfBm25 = (tf * (K1 + 1.0)) / (tf + norm);
                accum.merge(node.docId, idf * tfBm25, Double::sum);
                node = node.next;
            }
        }

        List<ScoredDocument> results = new ArrayList<>(accum.size());
        for (Map.Entry<Integer, Double> e : accum.entrySet()) {
            results.add(new ScoredDocument(e.getKey(), e.getValue()));
        }
        return results;
    }

    /** BM25 score for a single term in a single document. */
    public double score(String term, int docId) {
        int N  = index.getDocCount();
        int df = index.df(term);
        int tf = index.tf(term, docId);
        if (N == 0 || df == 0 || tf == 0) return 0.0;

        double idf  = Math.log((N - df + 0.5) / (df + 0.5) + 1.0);
        double dl   = index.getDocLength(docId);
        double norm = K1 * (1.0 - B + B * dl / avgDocLength);
        return idf * (tf * (K1 + 1.0)) / (tf + norm);
    }
}
