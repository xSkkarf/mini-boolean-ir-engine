package it.unipv.ir.ranking;

import it.unipv.ir.datastructures.PostingList;
import it.unipv.ir.datastructures.PostingNode;
import it.unipv.ir.index.InvertedIndex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TF-IDF scoring.
 *
 * Formula:
 *   tf-idf(t, d) = tf_weight(t,d) * idf_weight(t)
 *
 *   tf_weight  = 1 + log10(tf)   if tf > 0, else 0    (log normalisation)
 *   idf_weight = log10(N / df)                          (inverse doc freq)
 *
 *   document score = sum over query terms of tf-idf(t, d)
 *
 * Documents are scored using the standard cosine-like accumulator approach:
 * iterate over each query term's posting list and accumulate scores by docId.
 */
public class TfIdfScorer {

    private final InvertedIndex index;

    public TfIdfScorer(InvertedIndex index) {
        this.index = index;
    }

    /**
     * Score all documents containing at least one query term.
     *
     * @param queryTerms preprocessed, stemmed query terms
     * @return list of ScoredDocument, unsorted (call TopKHeap to rank)
     */
    public List<ScoredDocument> score(List<String> queryTerms) {
        int N = index.getDocCount();
        if (N == 0) return List.of();

        // Accumulator: docId → accumulated score
        Map<Integer, Double> accum = new HashMap<>();

        for (String term : queryTerms) {
            PostingList pl = index.getPostingList(term);
            if (pl == null) continue;

            int df = pl.getSize();
            if (df == 0) continue;

            double idf = Math.log10((double) N / df);

            PostingNode node = pl.getHead();
            while (node != null) {
                double tf  = node.tf > 0 ? 1.0 + Math.log10(node.tf) : 0.0;
                double contribution = tf * idf;
                accum.merge(node.docId, contribution, Double::sum);
                node = node.next;
            }
        }

        List<ScoredDocument> results = new ArrayList<>(accum.size());
        for (Map.Entry<Integer, Double> e : accum.entrySet()) {
            results.add(new ScoredDocument(e.getKey(), e.getValue()));
        }
        return results;
    }

    /** Compute tf-idf score for a single term in a single document. */
    public double score(String term, int docId) {
        int N  = index.getDocCount();
        int df = index.df(term);
        int tf = index.tf(term, docId);
        if (N == 0 || df == 0 || tf == 0) return 0.0;
        double tfWeight  = 1.0 + Math.log10(tf);
        double idfWeight = Math.log10((double) N / df);
        return tfWeight * idfWeight;
    }
}
