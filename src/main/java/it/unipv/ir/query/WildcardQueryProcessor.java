package it.unipv.ir.query;

import it.unipv.ir.datastructures.PostingList;
import it.unipv.ir.datastructures.SkipList;
import it.unipv.ir.index.InvertedIndex;
import it.unipv.ir.index.KGramIndex;
import it.unipv.ir.index.PermutermIndex;

import java.util.Set;

/**
 * Processes wildcard queries containing one or more '*' characters.
 *
 * Two complementary approaches are used:
 *
 * 1. PermutermIndex — fast exact resolution via rotation:
 *    "comp*ter" → rotate → "ter$comp" → prefix lookup in Trie.
 *    Best for patterns with a small result set.
 *
 * 2. KGramIndex — approximate resolution via gram overlap:
 *    Extract k-grams from the pattern, intersect candidate sets,
 *    then post-filter with regex.
 *    More robust for patterns that span many rotations.
 *
 * Both approaches return a set of vocabulary terms. Their union is used
 * to build the final PostingList by unioning postings for all matched terms.
 */
public class WildcardQueryProcessor {

    private final InvertedIndex   index;
    private final PermutermIndex  permutermIndex;
    private final KGramIndex      kgramIndex;

    public WildcardQueryProcessor(InvertedIndex index,
                                   PermutermIndex permutermIndex,
                                   KGramIndex kgramIndex) {
        this.index          = index;
        this.permutermIndex = permutermIndex;
        this.kgramIndex     = kgramIndex;
    }

    /**
     * Resolve a wildcard pattern and return the unioned PostingList
     * of all matching vocabulary terms.
     */
    public PostingList resolve(String pattern) {
        // Use permuterm for single-wildcard patterns
        Set<String> terms = permutermIndex.resolve(pattern);

        // Fall back / supplement with kgram if permuterm finds nothing
        if (terms.isEmpty()) {
            terms = kgramIndex.resolveWildcard(pattern);
        }

        // Union posting lists for all matched terms
        PostingList result = new PostingList();
        for (String term : terms) {
            PostingList pl = index.getPostingList(term);
            if (pl != null) {
                result = SkipList.union(result, pl);
            }
        }
        return result;
    }

    /**
     * Returns the set of vocabulary terms matching the wildcard pattern.
     * Useful for query expansion and debugging.
     */
    public Set<String> expandPattern(String pattern) {
        Set<String> terms = permutermIndex.resolve(pattern);
        if (terms.isEmpty()) terms = kgramIndex.resolveWildcard(pattern);
        return terms;
    }
}
