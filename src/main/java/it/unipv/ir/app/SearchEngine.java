package it.unipv.ir.app;

import it.unipv.ir.crawler.DocumentStore;
import it.unipv.ir.crawler.RawDocument;
import it.unipv.ir.datastructures.PostingList;
import it.unipv.ir.datastructures.PostingNode;
import it.unipv.ir.query.*;
import it.unipv.ir.ranking.BM25Scorer;
import it.unipv.ir.ranking.ScoredDocument;
import it.unipv.ir.ranking.TfIdfScorer;
import it.unipv.ir.ranking.TopKHeap;
import it.unipv.ir.similarity.SimilarityChecker;
import it.unipv.ir.spelling.SpellingCorrector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Facade for the entire search pipeline.
 *
 * Usage:
 *   SearchEngine engine = new SearchEngine(bundle, store);
 *   List<SearchResult> results = engine.search("quantum mechanics", 10);
 *
 * Handles query parsing, dispatch to the correct processor, BM25 ranking,
 * and spelling correction suggestion when no results are found.
 */
public class SearchEngine {

    private static final int DEFAULT_TOP_K = 10;

    private final IndexBundle          bundle;
    private final DocumentStore        store;

    private final QueryParser              parser;
    private final BooleanQueryProcessor    boolProcessor;
    private final PhraseQueryProcessor     phraseProcessor;
    private final WildcardQueryProcessor   wildcardProcessor;
    private final ProximityQueryProcessor  proximityProcessor;
    private final BM25Scorer               bm25;
    private final TfIdfScorer              tfidf;
    private final TopKHeap                 topK;
    private final SpellingCorrector        corrector;
    private final SimilarityChecker        simChecker;

    public SearchEngine(IndexBundle bundle, DocumentStore store) {
        this.bundle = bundle;
        this.store  = store;

        parser            = new QueryParser();
        boolProcessor     = new BooleanQueryProcessor(
            bundle.positionalIndex, bundle.pipeline);
        phraseProcessor   = new PhraseQueryProcessor(
            bundle.positionalIndex, bundle.pipeline);
        wildcardProcessor = new WildcardQueryProcessor(
            bundle.positionalIndex, bundle.permutermIndex, bundle.kgramIndex);
        proximityProcessor = new ProximityQueryProcessor(
            bundle.positionalIndex, bundle.pipeline);
        bm25    = new BM25Scorer(bundle.positionalIndex);
        tfidf   = new TfIdfScorer(bundle.positionalIndex);
        topK    = new TopKHeap(DEFAULT_TOP_K);
        corrector = new SpellingCorrector(
            bundle.positionalIndex, bundle.kgramIndex);
        simChecker = new SimilarityChecker(bundle.fingerprintBuilder);
    }

    // -------------------------------------------------------------------------
    // Main search entry point
    // -------------------------------------------------------------------------

    /**
     * Execute a query and return up to k ranked results.
     * Automatically dispatches to the correct processor based on query type.
     */
    public SearchResponse search(String rawQuery, int k) throws IOException {
        QueryParser.ParsedQuery parsed = parser.parse(rawQuery);
        PostingList hits;

        switch (parsed.type) {
            case PHRASE:
                hits = phraseProcessor.phraseQuery(parsed.terms);
                break;
            case WILDCARD:
                hits = wildcardProcessor.resolve(parsed.wildcard);
                break;
            case PROXIMITY:
                hits = proximityProcessor.proximity(parsed.terms, parsed.proximityK);
                break;
            case DISJUNCTIVE:
                hits = boolProcessor.disjunctive(parsed.terms);
                break;
            case PARAMETRIC:
                hits = bundle.parametricIndex.lookup(parsed.field,
                    parsed.terms.isEmpty() ? "" : parsed.terms.get(0));
                break;
            case CONJUNCTIVE:
            default:
                if (parsed.hasNot) {
                    hits = boolProcessor.andNot(parsed.terms, parsed.notTerms);
                } else {
                    hits = boolProcessor.conjunctive(parsed.terms);
                }
        }

        // If no results, suggest spelling corrections
        List<String> suggestions = new ArrayList<>();
        if (hits.isEmpty() && parsed.type == QueryParser.QueryType.CONJUNCTIVE) {
            for (String term : parsed.terms) {
                String best = corrector.bestSuggestion(term);
                if (!best.equals(term)) suggestions.add(term + " → " + best);
            }
        }

        // Rank results using BM25
        List<String> processedTerms = new ArrayList<>();
        for (String t : parsed.terms)
            processedTerms.addAll(bundle.pipeline.process(t));

        List<ScoredDocument> scores = bm25.score(processedTerms);

        // Intersect with boolean hit set
        List<ScoredDocument> filtered = filterByHits(scores, hits);

        // Extract top-K
        TopKHeap topKHeap = new TopKHeap(k);
        List<ScoredDocument> ranked = topKHeap.topK(filtered);

        // Resolve docIds to titles
        List<SearchResult> results = new ArrayList<>();
        for (ScoredDocument sd : ranked) {
            RawDocument doc = store.load(sd.getDocId());
            String title = doc != null ? doc.getTitle() : "Doc#" + sd.getDocId();
            String url   = doc != null ? doc.getUrl()   : "";
            results.add(new SearchResult(sd.getDocId(), title, url, sd.getScore()));
        }

        return new SearchResponse(rawQuery, results, suggestions,
            hits.getSize());
    }

    public SearchResponse search(String rawQuery) throws IOException {
        return search(rawQuery, DEFAULT_TOP_K);
    }

    /**
     * Find documents similar to a given document (by fingerprint Jaccard).
     */
    public List<SimilarityChecker.SimilarDoc> findSimilar(int docId,
                                                           double threshold) {
        return simChecker.findSimilar(docId, threshold);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private List<ScoredDocument> filterByHits(List<ScoredDocument> scores,
                                               PostingList hits) {
        if (hits.isEmpty()) return scores;

        // Build a fast lookup set of matching docIds
        java.util.Set<Integer> hitSet = new java.util.HashSet<>();
        PostingNode node = hits.getHead();
        while (node != null) { hitSet.add(node.docId); node = node.next; }

        // Filter scored documents to only those in hits
        List<ScoredDocument> filtered = scores.stream()
            .filter(sd -> hitSet.contains(sd.getDocId()))
            .collect(Collectors.toList());

        // If BM25 returned nothing (e.g. for phrase query), score hits at 1.0
        if (filtered.isEmpty()) {
            node = hits.getHead();
            while (node != null) {
                filtered.add(new ScoredDocument(node.docId, (double) node.tf));
                node = node.next;
            }
        }
        return filtered;
    }

    // -------------------------------------------------------------------------
    // Result value objects
    // -------------------------------------------------------------------------

    public static class SearchResult {
        public final int    docId;
        public final String title;
        public final String url;
        public final double score;

        SearchResult(int docId, String title, String url, double score) {
            this.docId = docId;
            this.title = title;
            this.url   = url;
            this.score = score;
        }

        @Override
        public String toString() {
            return String.format("[%.3f] %s  (%s)", score, title, url);
        }
    }

    public static class SearchResponse {
        public final String            query;
        public final List<SearchResult> results;
        public final List<String>       suggestions;
        public final int               totalHits;

        SearchResponse(String query, List<SearchResult> results,
                       List<String> suggestions, int totalHits) {
            this.query       = query;
            this.results     = results;
            this.suggestions = suggestions;
            this.totalHits   = totalHits;
        }
    }
}
