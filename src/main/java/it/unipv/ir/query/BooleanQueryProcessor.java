package it.unipv.ir.query;

import it.unipv.ir.datastructures.PostingList;
import it.unipv.ir.datastructures.SkipList;
import it.unipv.ir.index.InvertedIndex;
import it.unipv.ir.preprocessing.PreprocessingPipeline;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Processes boolean queries: conjunctive (AND), disjunctive (OR),
 * and AND NOT.
 *
 * Conjunctive optimisation (Manning et al. §1.3):
 *   Sort posting lists by ascending document frequency before merging.
 *   Start with the smallest list — early termination cuts work significantly
 *   for selective queries in a 10k-document corpus.
 *
 * All query terms are preprocessed (normalised + stemmed) before lookup
 * so they match the indexed forms.
 */
public class BooleanQueryProcessor {

    private final InvertedIndex       index;
    private final PreprocessingPipeline pipeline;

    public BooleanQueryProcessor(InvertedIndex index,
                                  PreprocessingPipeline pipeline) {
        this.index    = index;
        this.pipeline = pipeline;
    }

    // -------------------------------------------------------------------------
    // Conjunctive (AND) — optimised
    // -------------------------------------------------------------------------

    /**
     * Return a PostingList of docIds matching ALL query terms.
     * Terms are preprocessed before lookup.
     */
    public PostingList conjunctive(List<String> rawTerms) {
        List<String> terms = preprocess(rawTerms);
        if (terms.isEmpty()) return new PostingList();

        // Gather posting lists and sort by df ascending
        List<PostingList> lists = new ArrayList<>();
        for (String term : terms) {
            PostingList pl = index.getPostingList(term);
            if (pl == null) return new PostingList(); // one missing → result is empty
            lists.add(pl);
        }

        // Sort by df ascending (optimisation)
        lists.sort(Comparator.comparingInt(PostingList::getSize));

        // Merge pairwise from smallest
        PostingList result = lists.get(0);
        for (int i = 1; i < lists.size(); i++) {
            result = SkipList.intersect(result, lists.get(i));
            if (result.isEmpty()) return result; // early termination
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Disjunctive (OR)
    // -------------------------------------------------------------------------

    public PostingList disjunctive(List<String> rawTerms) {
        List<String> terms = preprocess(rawTerms);
        if (terms.isEmpty()) return new PostingList();

        PostingList result = new PostingList();
        for (String term : terms) {
            PostingList pl = index.getPostingList(term);
            if (pl != null) {
                result = SkipList.union(result, pl);
            }
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // AND NOT
    // -------------------------------------------------------------------------

    /**
     * Return docIds matching all positiveTerms but none of notTerms.
     */
    public PostingList andNot(List<String> rawPositive, List<String> rawNot) {
        PostingList positive = conjunctive(rawPositive);
        if (positive.isEmpty()) return positive;

        PostingList negative = disjunctive(rawNot);
        return SkipList.difference(positive, negative);
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    /** Preprocess (normalise + stem) a list of raw query terms. */
    private List<String> preprocess(List<String> rawTerms) {
        List<String> result = new ArrayList<>();
        for (String t : rawTerms) {
            List<String> processed = pipeline.process(t);
            result.addAll(processed);
        }
        return result;
    }
}
