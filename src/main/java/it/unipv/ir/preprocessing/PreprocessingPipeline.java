package it.unipv.ir.preprocessing;

import java.io.IOException;
import java.util.List;

/**
 * Chains all preprocessing steps into a single pipeline:
 *
 *   raw text
 *     → Tokenizer     (split on non-alphanumeric)
 *     → LinguisticNormalizer    (lowercase, strip diacritics)
 *     → StopWordFilter (remove function words)
 *     → PorterStemmer  (reduce to morphological stem)
 *     → List<String>  (processed terms, in document order)
 *
 * Also exposes a processRaw() variant that skips stemming so the
 * original forms are available for the zone/parametric indexes.
 */
public class PreprocessingPipeline {

    private final Tokenizer       tokenizer;
    private final LinguisticNormalizer normalizer;
    private final StopWordFilter  stopWordFilter;
    private final PorterStemmer   stemmer;

    public PreprocessingPipeline() throws IOException {
        this.tokenizer      = new Tokenizer();
        this.normalizer     = new LinguisticNormalizer();
        this.stopWordFilter = new StopWordFilter();
        this.stemmer        = new PorterStemmer();
    }

    /**
     * Full pipeline: tokenize → normalize → stop-filter → stem.
     * Returns terms in original document order (positions preserved).
     */
    public List<String> process(String text) {
        List<String> tokens = tokenizer.tokenize(text);
        tokens = normalizer.normalize(tokens);
        tokens = stopWordFilter.filter(tokens);
        tokens = stemmer.stemAll(tokens);
        return tokens;
    }

    /**
     * Tokenize + normalize only (no stop removal, no stemming).
     * Used when building the zone index title fields — we want
     * the original normalised forms for display.
     */
    public List<String> processRaw(String text) {
        List<String> tokens = tokenizer.tokenize(text);
        return normalizer.normalize(tokens);
    }

    // Expose individual components for testing and reuse
    public Tokenizer      getTokenizer()      { return tokenizer; }
    public LinguisticNormalizer getNormalizer()     { return normalizer; }
    public StopWordFilter getStopWordFilter() { return stopWordFilter; }
    public PorterStemmer  getStemmer()        { return stemmer; }
}
