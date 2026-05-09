package it.unipv.ir.spelling;

import it.unipv.ir.index.InvertedIndex;
import it.unipv.ir.index.KGramIndex;

import java.util.List;

/**
 * Public facade for spelling correction.
 *
 * For each unrecognised query term, generates up to topN correction
 * suggestions ranked by edit distance. If the term is already in the
 * index vocabulary, it is returned as-is with distance 0.
 *
 * Usage:
 *   SpellingCorrector sc = new SpellingCorrector(index, kgramIndex);
 *   List<CorrectionResult> suggestions = sc.suggest("quantom", 5);
 *   // → [quantum(ed=1), quant(ed=2), ...]
 */
public class SpellingCorrector {

    private static final int DEFAULT_TOP_N = 5;

    private final InvertedIndex      index;
    private final CandidateGenerator generator;

    public SpellingCorrector(InvertedIndex index, KGramIndex kgramIndex) {
        this.index     = index;
        this.generator = new CandidateGenerator(kgramIndex);
    }

    /**
     * Return correction suggestions for a single word.
     * If the word is in the index, returns it as the only suggestion.
     */
    public List<CorrectionResult> suggest(String word) {
        return suggest(word, DEFAULT_TOP_N);
    }

    public List<CorrectionResult> suggest(String word, int topN) {
        // Already in index — no correction needed
        if (index.getPostingList(word) != null) {
            return List.of(new CorrectionResult(word, 0));
        }
        return generator.generate(word, topN);
    }

    /**
     * Return the single best suggestion (lowest edit distance).
     * Returns the original word if no suggestion is found.
     */
    public String bestSuggestion(String word) {
        List<CorrectionResult> suggestions = suggest(word, 1);
        if (suggestions.isEmpty()) return word;
        return suggestions.get(0).getTerm();
    }

    /**
     * Check if a word is in the index vocabulary.
     */
    public boolean isKnown(String word) {
        return index.getPostingList(word) != null;
    }
}
