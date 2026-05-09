package it.unipv.ir.app;

import it.unipv.ir.compression.FrontCodingDictionary;
import it.unipv.ir.compression.PostingCompressor;
import it.unipv.ir.crawler.DocumentStore;
import it.unipv.ir.crawler.RawDocument;
import it.unipv.ir.index.*;
import it.unipv.ir.preprocessing.PreprocessingPipeline;
import it.unipv.ir.similarity.FingerprintBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Builds all indexes from a DocumentStore.
 *
 * Build order:
 *   1. Load every RawDocument from disk.
 *   2. For each document:
 *      a. Preprocess body → terms (full pipeline)
 *      b. Preprocess title, categories → raw tokens (no stemming)
 *      c. Feed terms into InvertedIndex / PositionalIndex
 *      d. Feed into TieredIndex, BiWordIndex
 *      e. Feed zones into ZoneIndex
 *      f. Feed metadata into ParametricIndex
 *      g. Feed terms into FingerprintBuilder
 *   3. Finalise InvertedIndex (install skip pointers).
 *   4. Build KGramIndex and PermutermIndex from vocabulary.
 *   5. Apply posting list compression (VByte) and front-coding dictionary.
 *   6. Return an IndexBundle with all built structures.
 */
public class IndexBuilder {

    private static final Logger LOG =
        Logger.getLogger(IndexBuilder.class.getName());

    private final PreprocessingPipeline pipeline;

    public IndexBuilder() throws IOException {
        this.pipeline = new PreprocessingPipeline();
    }

    /**
     * Build all indexes from all documents in the store.
     * This is the single entry point called by Main.
     */
    public IndexBundle build(DocumentStore store) throws IOException {
        LOG.info("Loading documents from store...");
        List<RawDocument> docs = store.loadAll();
        LOG.info("Loaded " + docs.size() + " documents. Building indexes...");

        // Initialise all indexes
        PositionalIndex  positional  = new PositionalIndex();
        TieredIndex      tiered      = new TieredIndex();
        BiWordIndex      biword      = new BiWordIndex();
        ZoneIndex        zone        = new ZoneIndex();
        ParametricIndex  parametric  = new ParametricIndex();
        FingerprintBuilder fingerprints = new FingerprintBuilder();

        int processed = 0;
        for (RawDocument doc : docs) {
            int docId = doc.getDocId();

            // --- Full pipeline: body ---
            List<String> bodyTerms = pipeline.process(doc.getBody());

            // --- Raw tokens: title and categories (no stemming) ---
            List<String> titleTokens = pipeline.processRaw(doc.getTitle());
            List<String> catTokens   = categoriesToTokens(doc.getCategories());

            // Full-pipeline title for main index
            List<String> titleTerms  = pipeline.process(doc.getTitle());

            // Combine title + body for main inverted index
            // Title terms are prepended so they contribute to positional index too
            List<String> allTerms = new ArrayList<>();
            allTerms.addAll(titleTerms);
            allTerms.addAll(bodyTerms);

            // 1. Positional / Inverted index (positional extends inverted)
            positional.indexDocument(docId, allTerms);

            // 2. Tiered index
            tiered.indexDocument(docId, allTerms);

            // 3. BiWord index
            biword.indexDocument(docId, allTerms);

            // 4. Zone index
            zone.indexZone(ZoneIndex.ZONE_TITLE,      docId, titleTokens);
            zone.indexZone(ZoneIndex.ZONE_BODY,       docId,
                pipeline.getTokenizer().tokenize(
                    pipeline.getNormalizer()
                        .normalizeToken(doc.getBody().substring(
                            0, Math.min(500, doc.getBody().length())))));
            zone.indexZone(ZoneIndex.ZONE_CATEGORIES, docId, catTokens);

            // 5. Parametric index
            parametric.indexField(ParametricIndex.FIELD_TITLE,
                docId, titleTokens);
            parametric.indexField(ParametricIndex.FIELD_CATEGORY,
                docId, catTokens);

            // 6. Fingerprint
            fingerprints.addDocument(docId, allTerms);

            processed++;
            if (processed % 500 == 0) {
                LOG.info("Indexed " + processed + " / " + docs.size()
                    + " documents...");
            }
        }

        // Finalise: install skip pointers
        LOG.info("Installing skip pointers...");
        positional.finalise();

        // Build wildcard indexes from vocabulary
        LOG.info("Building k-gram and permuterm indexes...");
        KGramIndex     kgram     = new KGramIndex();
        PermutermIndex permuterm = new PermutermIndex();

        // Walk all terms in the B-Tree dictionary
        collectVocabulary(positional).forEach(term -> {
            kgram.addTerm(term);
            permuterm.addTerm(term);
        });

        LOG.info("Vocabulary size: " + positional.getTermCount() + " terms");

        // Compression
        LOG.info("Compressing posting lists...");
        PostingCompressor compressor = new PostingCompressor();
        PostingCompressor.CompressionStats stats = compressor.compress(positional);
        LOG.info(stats.toString());

        LOG.info("Building front-coding dictionary...");
        FrontCodingDictionary fcDict = new FrontCodingDictionary();
        List<String> sortedVocab = new ArrayList<>(collectVocabulary(positional));
        java.util.Collections.sort(sortedVocab);
        fcDict.build(sortedVocab);

        LOG.info("All indexes built successfully.");

        return new IndexBundle(
            positional, tiered, biword, zone, parametric,
            kgram, permuterm, fingerprints,
            compressor, fcDict, pipeline
        );
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private List<String> categoriesToTokens(List<String> categories) {
        if (categories == null) return List.of();
        List<String> tokens = new ArrayList<>();
        for (String cat : categories) {
            tokens.addAll(pipeline.processRaw(cat));
        }
        return tokens;
    }

    private List<String> collectVocabulary(PositionalIndex index) {
        List<String> vocab = new ArrayList<>();
        collectFromNode(index.getDictionary().getRoot(), vocab);
        return vocab;
    }

    private void collectFromNode(
            it.unipv.ir.datastructures.BTreeDictionary.BTreeNode node,
            List<String> vocab) {
        if (node == null) return;
        for (int i = 0; i < node.keyCount; i++) {
            if (node.keys[i] != null) vocab.add(node.keys[i]);
            if (!node.isLeaf) collectFromNode(node.children[i], vocab);
        }
        if (!node.isLeaf) collectFromNode(node.children[node.keyCount], vocab);
    }
}
