package it.unipv.ir.spelling;

import it.unipv.ir.index.KGramIndex;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

// =============================================================================
// CandidateGenerator.java — embedded as a package-private helper class.
// Exported as its own public class below.
// =============================================================================

/**
 * Generates spelling correction candidates for a misspelled word using
 * the two-stage pipeline recommended by Manning et al. §3.3:
 *
 * Stage 1 — k-gram overlap filter (fast):
 *   Compute trigrams of the input word. Retrieve vocabulary terms sharing
 *   at least MIN_OVERLAP trigrams. This prunes the candidate set from
 *   ~50k vocabulary terms down to a few hundred.
 *
 * Stage 2 — edit distance ranking (precise):
 *   Sort the candidate set by Levenshtein distance to the input word.
 *   Return the top-N candidates within MAX_EDIT_DISTANCE.
 */
class CandidateGenerator {

    private static final int MIN_OVERLAP      = 1;  // minimum shared trigrams
    private static final int MAX_EDIT_DISTANCE = 2;  // max edit distance to consider

    private final KGramIndex    kgramIndex;
    private final EditDistance  editDistance;

    CandidateGenerator(KGramIndex kgramIndex) {
        this.kgramIndex   = kgramIndex;
        this.editDistance = new EditDistance();
    }

    /**
     * Generate ranked correction candidates for a word.
     *
     * @param word  the misspelled (or possibly correct) word
     * @param topN  maximum number of candidates to return
     */
    List<CorrectionResult> generate(String word, int topN) {
        // Stage 1: k-gram candidate set
        Set<String> candidates = kgramIndex.getCandidatesByOverlap(
            word, MIN_OVERLAP);

        // If the vocabulary is small or k-gram returns nothing, fall back
        // to full vocabulary scan (slower but ensures recall)
        if (candidates.isEmpty()) {
            candidates = kgramIndex.getVocabulary();
        }

        // Stage 2: compute edit distances and filter
        List<CorrectionResult> results = new ArrayList<>();
        for (String candidate : candidates) {
            int ed = editDistance.distance(word, candidate);
            if (ed <= MAX_EDIT_DISTANCE) {
                results.add(new CorrectionResult(candidate, ed));
            }
        }

        // Sort: lowest edit distance first, then alphabetical for ties
        results.sort(Comparator
            .comparingInt(CorrectionResult::getEditDistance)
            .thenComparing(CorrectionResult::getTerm));

        return results.subList(0, Math.min(topN, results.size()));
    }
}
