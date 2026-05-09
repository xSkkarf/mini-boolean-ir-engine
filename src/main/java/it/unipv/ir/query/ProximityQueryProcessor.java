package it.unipv.ir.query;

import it.unipv.ir.datastructures.PostingList;
import it.unipv.ir.index.PositionalIndex;
import it.unipv.ir.preprocessing.PreprocessingPipeline;

import java.util.List;

/**
 * Processes proximity queries of the form "term1 NEAR/k term2".
 *
 * Two terms are within proximity k if they appear within k token positions
 * of each other in the same document (in either order).
 *
 * Uses the PositionalIndex's positional merge algorithm.
 */
public class ProximityQueryProcessor {

    private final PositionalIndex     index;
    private final PreprocessingPipeline pipeline;

    public ProximityQueryProcessor(PositionalIndex index,
                                    PreprocessingPipeline pipeline) {
        this.index    = index;
        this.pipeline = pipeline;
    }

    /**
     * Execute a proximity query.
     *
     * @param rawTerms  exactly two terms from the NEAR/k query
     * @param k         maximum allowed positional distance
     * @return PostingList of matching docIds
     */
    public PostingList proximity(List<String> rawTerms, int k) {
        if (rawTerms.size() < 2) return new PostingList();

        List<String> t1 = pipeline.process(rawTerms.get(0));
        List<String> t2 = pipeline.process(rawTerms.get(1));

        if (t1.isEmpty() || t2.isEmpty()) return new PostingList();

        return index.proximityQuery(t1.get(0), t2.get(0), k);
    }
}
