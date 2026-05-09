package it.unipv.ir.app;

import it.unipv.ir.compression.FrontCodingDictionary;
import it.unipv.ir.compression.PostingCompressor;
import it.unipv.ir.index.*;
import it.unipv.ir.preprocessing.PreprocessingPipeline;
import it.unipv.ir.similarity.FingerprintBuilder;

/**
 * Immutable container holding all built indexes and shared components.
 * Passed from IndexBuilder to SearchEngine and Main.
 */
public class IndexBundle {

    public final PositionalIndex       positionalIndex;
    public final TieredIndex           tieredIndex;
    public final BiWordIndex           biWordIndex;
    public final ZoneIndex             zoneIndex;
    public final ParametricIndex       parametricIndex;
    public final KGramIndex            kgramIndex;
    public final PermutermIndex        permutermIndex;
    public final FingerprintBuilder    fingerprintBuilder;
    public final PostingCompressor     postingCompressor;
    public final FrontCodingDictionary frontCodingDict;
    public final PreprocessingPipeline pipeline;

    public IndexBundle(
            PositionalIndex       positionalIndex,
            TieredIndex           tieredIndex,
            BiWordIndex           biWordIndex,
            ZoneIndex             zoneIndex,
            ParametricIndex       parametricIndex,
            KGramIndex            kgramIndex,
            PermutermIndex        permutermIndex,
            FingerprintBuilder    fingerprintBuilder,
            PostingCompressor     postingCompressor,
            FrontCodingDictionary frontCodingDict,
            PreprocessingPipeline pipeline) {
        this.positionalIndex    = positionalIndex;
        this.tieredIndex        = tieredIndex;
        this.biWordIndex        = biWordIndex;
        this.zoneIndex          = zoneIndex;
        this.parametricIndex    = parametricIndex;
        this.kgramIndex         = kgramIndex;
        this.permutermIndex     = permutermIndex;
        this.fingerprintBuilder = fingerprintBuilder;
        this.postingCompressor  = postingCompressor;
        this.frontCodingDict    = frontCodingDict;
        this.pipeline           = pipeline;
    }
}
