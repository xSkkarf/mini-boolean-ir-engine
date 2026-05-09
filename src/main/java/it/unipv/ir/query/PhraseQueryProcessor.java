package it.unipv.ir.query;

import it.unipv.ir.datastructures.PostingList;
import it.unipv.ir.index.PositionalIndex;
import it.unipv.ir.preprocessing.PreprocessingPipeline;

import java.util.List;

/**
 * Processes phrase queries and proximity queries using the PositionalIndex.
 *
 * Phrase query:  "new york city" → terms must appear consecutively.
 * Proximity query: "new NEAR/3 york" → terms within 3 positions.
 *
 * Both query types require the positional index (positions per docId per term).
 */
public class PhraseQueryProcessor {

    private final PositionalIndex     index;
    private final PreprocessingPipeline pipeline;

    public PhraseQueryProcessor(PositionalIndex index,
                                 PreprocessingPipeline pipeline) {
        this.index    = index;
        this.pipeline = pipeline;
    }

    /**
     * Execute a phrase query. Terms are preprocessed (stemmed) before lookup.
     * Returns docIds where the exact phrase appears.
     */
    public PostingList phraseQuery(List<String> rawTerms) {
        List<String> terms = preprocess(rawTerms);
        if (terms.isEmpty()) return new PostingList();
        return index.phraseQuery(terms);
    }

    /**
     * Execute a proximity query: term1 within k positions of term2.
     */
    public PostingList proximityQuery(String rawTerm1, String rawTerm2, int k) {
        List<String> t1 = preprocess(List.of(rawTerm1));
        List<String> t2 = preprocess(List.of(rawTerm2));
        if (t1.isEmpty() || t2.isEmpty()) return new PostingList();
        return index.proximityQuery(t1.get(0), t2.get(0), k);
    }

    private List<String> preprocess(List<String> rawTerms) {
        java.util.List<String> result = new java.util.ArrayList<>();
        for (String t : rawTerms) result.addAll(pipeline.process(t));
        return result;
    }
}
