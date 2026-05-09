package it.unipv.ir;

import it.unipv.ir.compression.FrontCodingDictionary;
import it.unipv.ir.compression.VariableByteEncoder;
import it.unipv.ir.datastructures.*;
import it.unipv.ir.index.*;
import it.unipv.ir.preprocessing.*;
import it.unipv.ir.query.*;
import it.unipv.ir.ranking.*;
import it.unipv.ir.similarity.*;
import it.unipv.ir.spelling.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for all core IR components.
 * Run with: mvn test
 */
class IRSystemTest {

    // =========================================================================
    // Data Structures
    // =========================================================================

    @Test
    void postingList_addAndFind() {
        PostingList pl = new PostingList();
        pl.add(1, 3);
        pl.add(1, 7);   // same doc, second position
        pl.add(5, 2);
        pl.add(9, 1);

        assertEquals(3, pl.getSize());

        PostingNode n1 = pl.find(1);
        assertNotNull(n1);
        assertEquals(2, n1.tf);
        assertEquals(List.of(3, 7), n1.positions);

        assertNull(pl.find(4));   // not in list
        assertNotNull(pl.find(9));
    }

    @Test
    void skipList_intersect() {
        PostingList p1 = new PostingList();
        PostingList p2 = new PostingList();
        for (int i : new int[]{1, 3, 5, 7, 9})  p1.addDocId(i);
        for (int i : new int[]{2, 3, 5, 8, 9})  p2.addDocId(i);
        SkipList.installSkips(p1);
        SkipList.installSkips(p2);

        PostingList result = SkipList.intersect(p1, p2);
        assertEquals(List.of(3, 5, 9), result.toDocIdList());
    }

    @Test
    void skipList_union() {
        PostingList p1 = new PostingList();
        PostingList p2 = new PostingList();
        for (int i : new int[]{1, 3, 5})  p1.addDocId(i);
        for (int i : new int[]{2, 3, 6})  p2.addDocId(i);

        PostingList result = SkipList.union(p1, p2);
        assertEquals(List.of(1, 2, 3, 5, 6), result.toDocIdList());
    }

    @Test
    void skipList_difference() {
        PostingList p1 = new PostingList();
        PostingList p2 = new PostingList();
        for (int i : new int[]{1, 2, 3, 4, 5})  p1.addDocId(i);
        for (int i : new int[]{2, 4})            p2.addDocId(i);

        PostingList result = SkipList.difference(p1, p2);
        assertEquals(List.of(1, 3, 5), result.toDocIdList());
    }

    @Test
    void bTreeDictionary_putGetRangePrefix() {
        BTreeDictionary dict = new BTreeDictionary();
        PostingList pl1 = new PostingList(); pl1.addDocId(1);
        PostingList pl2 = new PostingList(); pl2.addDocId(2);
        PostingList pl3 = new PostingList(); pl3.addDocId(3);

        dict.put("apple",  pl1);
        dict.put("apply",  pl2);
        dict.put("banana", pl3);

        assertNotNull(dict.get("apple"));
        assertNull(dict.get("mango"));

        List<String> prefix = dict.prefixKeys("app");
        assertTrue(prefix.contains("apple"));
        assertTrue(prefix.contains("apply"));
        assertFalse(prefix.contains("banana"));
    }

    @Test
    void trie_insertLookupPrefix() {
        Trie trie = new Trie();
        trie.insert("he", "hello");
        trie.insert("he", "help");
        trie.insert("wo", "world");

        Set<String> he = trie.lookup("he");
        assertTrue(he.contains("hello"));
        assertTrue(he.contains("help"));

        Set<String> prefix = trie.prefixLookup("h");
        assertTrue(prefix.contains("hello"));
        assertTrue(prefix.contains("help"));
        assertFalse(prefix.contains("world"));
    }

    @Test
    void binaryHeap_maxHeap() {
        BinaryHeap<Integer> heap = new BinaryHeap<>();
        heap.insert(5); heap.insert(1); heap.insert(9); heap.insert(3);
        assertEquals(9, heap.extract());
        assertEquals(5, heap.extract());
    }

    @Test
    void binaryHeap_minHeap() {
        BinaryHeap<Integer> heap = new BinaryHeap<>(true);
        heap.insert(5); heap.insert(1); heap.insert(9); heap.insert(3);
        assertEquals(1, heap.extract());
        assertEquals(3, heap.extract());
    }

    // =========================================================================
    // Preprocessing
    // =========================================================================

    @Test
    void tokenizer_basic() {
        Tokenizer t = new Tokenizer();
        List<String> tokens = t.tokenize("Hello, World! This is a test.");
        assertTrue(tokens.contains("hello"));
        assertTrue(tokens.contains("world"));
        assertTrue(tokens.contains("test"));
        assertTrue(tokens.contains("is"));    // tokenizer keeps it, stop-filter would remove it later
    }

    @Test
    void porterStemmer_basicCases() {
        PorterStemmer s = new PorterStemmer();
        assertEquals("comput",   s.stem("computing"));
        assertEquals("comput",   s.stem("computer"));
        assertEquals("run",      s.stem("running"));
        assertEquals("happi",    s.stem("happiness"));
        assertEquals("connect",  s.stem("connection"));
    }

    @Test
    void stopWordFilter_removesStops() throws IOException {
        StopWordFilter f = new StopWordFilter();
        List<String> tokens = new ArrayList<>(
            Arrays.asList("the", "quantum", "of", "physics"));
        List<String> filtered = f.filter(tokens);
        assertFalse(filtered.contains("the"));
        assertFalse(filtered.contains("of"));
        assertTrue(filtered.contains("quantum"));
        assertTrue(filtered.contains("physics"));
    }

    // =========================================================================
    // Index
    // =========================================================================

    @Test
    void invertedIndex_indexAndLookup() {
        InvertedIndex idx = new InvertedIndex();
        idx.indexDocument(1, Arrays.asList("quantum", "physic", "quantum"));
        idx.indexDocument(2, Arrays.asList("classic", "physic"));

        assertEquals(2, idx.df("physic"));
        assertEquals(1, idx.df("quantum"));
        assertEquals(2, idx.tf("quantum", 1));
        assertEquals(1, idx.tf("physic",  2));
        assertNull(idx.getPostingList("banana"));
    }

    @Test
    void positionalIndex_phraseQuery() {
        PositionalIndex idx = new PositionalIndex();
        idx.indexDocument(1, Arrays.asList("new", "york", "citi"));
        idx.indexDocument(2, Arrays.asList("new", "delhi", "citi"));
        idx.indexDocument(3, Arrays.asList("york", "new", "citi"));

        PostingList result = idx.phraseQuery(Arrays.asList("new", "york"));
        assertEquals(List.of(1), result.toDocIdList());  // only doc 1 has "new york" consecutively
    }

    @Test
    void positionalIndex_proximityQuery() {
        PositionalIndex idx = new PositionalIndex();
        idx.indexDocument(1, Arrays.asList("quantum", "entangl", "physic"));
        idx.indexDocument(2, Arrays.asList("quantum", "a", "b", "c", "physic"));

        PostingList near2 = idx.proximityQuery("quantum", "physic", 2);
        assertEquals(List.of(1), near2.toDocIdList());  // doc 2 is too far apart

        PostingList near5 = idx.proximityQuery("quantum", "physic", 5);
        assertEquals(List.of(1, 2), near5.toDocIdList());
    }

    @Test
    void kgramIndex_resolveWildcard() {
        KGramIndex kg = new KGramIndex();
        kg.addTerm("computer");
        kg.addTerm("compute");
        kg.addTerm("computing");
        kg.addTerm("complete");

        Set<String> results = kg.resolveWildcard("com*");
        assertTrue(results.contains("computer"));
        assertTrue(results.contains("compute"));
    }

    @Test
    void permutermIndex_resolve() {
        PermutermIndex pi = new PermutermIndex();
        pi.addTerm("hello");
        pi.addTerm("help");
        pi.addTerm("world");

        Set<String> results = pi.resolve("hel*");
        assertTrue(results.contains("hello"));
        assertTrue(results.contains("help"));
        assertFalse(results.contains("world"));
    }

    // =========================================================================
    // Query
    // =========================================================================

    @Test
    void queryParser_detectsTypes() {
        QueryParser p = new QueryParser();

        assertEquals(QueryParser.QueryType.PHRASE,
            p.parse("\"quantum mechanics\"").type);
        assertEquals(QueryParser.QueryType.WILDCARD,
            p.parse("comp*ter").type);
        assertEquals(QueryParser.QueryType.DISJUNCTIVE,
            p.parse("quantum OR mechanics").type);
        assertEquals(QueryParser.QueryType.CONJUNCTIVE,
            p.parse("quantum mechanics").type);
        assertEquals(QueryParser.QueryType.PROXIMITY,
            p.parse("quantum NEAR/3 mechanics").type);
        assertEquals(3,
            p.parse("quantum NEAR/3 mechanics").proximityK);
    }

    @Test
    void booleanProcessor_conjunctiveOptimised() throws IOException {
        InvertedIndex idx = new InvertedIndex();
        idx.indexDocument(1, Arrays.asList("quantum", "physic", "mechan"));
        idx.indexDocument(2, Arrays.asList("quantum", "physic"));
        idx.indexDocument(3, Arrays.asList("quantum", "mechan"));
        idx.finalise();

        BooleanQueryProcessor proc =
            new BooleanQueryProcessor(idx, new PreprocessingPipeline());

        // Test with terms that are ALREADY preprocessed, to bypass pipeline.process in the loop
        PostingList result = proc.conjunctive(Arrays.asList("quantum", "physic"));
        assertEquals(2, result.getSize());
    }

    // =========================================================================
    // Spelling
    // =========================================================================

    @Test
    void editDistance_levenshtein() {
        EditDistance ed = new EditDistance();
        assertEquals(0, ed.distance("hello",  "hello"));
        assertEquals(1, ed.distance("hello",  "helo"));   // 1 delete
        assertEquals(1, ed.distance("kitten", "sitten")); // 1 substitute
        assertEquals(3, ed.distance("kitten", "sitting"));
        assertEquals(1, ed.distance("quantum", "quantom"));
    }

    // =========================================================================
    // Ranking
    // =========================================================================

    @Test
    void topKHeap_returnsTopK() {
        TopKHeap topK = new TopKHeap(3);
        List<ScoredDocument> docs = Arrays.asList(
            new ScoredDocument(1, 0.5),
            new ScoredDocument(2, 0.9),
            new ScoredDocument(3, 0.1),
            new ScoredDocument(4, 0.7),
            new ScoredDocument(5, 0.3)
        );
        List<ScoredDocument> top = topK.topK(docs);
        assertEquals(3, top.size());
        assertEquals(2, top.get(0).getDocId());  // 0.9 → rank 1
        assertEquals(4, top.get(1).getDocId());  // 0.7 → rank 2
        assertEquals(1, top.get(2).getDocId());  // 0.5 → rank 3
    }

    @Test
    void bm25_scoresHigherForHigherTf() {
        InvertedIndex idx = new InvertedIndex();
        // Doc 1 has "physic" 5 times, doc 2 only once
        List<String> doc1 = new ArrayList<>(Collections.nCopies(5, "physic"));
        doc1.add("other");
        idx.indexDocument(1, doc1);
        idx.indexDocument(2, Arrays.asList("physic", "other", "word"));
        idx.finalise();

        BM25Scorer bm25 = new BM25Scorer(idx);
        double score1 = bm25.score("physic", 1);
        double score2 = bm25.score("physic", 2);
        assertTrue(score1 > score2,
            "Higher tf should yield higher BM25 score");
    }

    // =========================================================================
    // Compression
    // =========================================================================

    @Test
    void vbyteEncoder_roundTrip() {
        VariableByteEncoder enc = new VariableByteEncoder();
        List<Integer> ids = Arrays.asList(3, 7, 11, 19, 100, 1000, 99999);
        byte[] compressed = enc.encodeDocIds(ids);
        List<Integer> decoded = enc.decodeDocIds(compressed);
        assertEquals(ids, decoded);
    }

    @Test
    void frontCoding_containsAfterBuild() {
        FrontCodingDictionary dict = new FrontCodingDictionary();
        List<String> vocab = Arrays.asList(
            "automate", "automatic", "automation", "automaton",
            "banana", "band", "bandana");
        dict.build(vocab);
        assertTrue(dict.contains("automate"));
        assertTrue(dict.contains("automatic"));
        assertTrue(dict.contains("banana"));
        assertFalse(dict.contains("mango"));
    }

    // =========================================================================
    // Similarity
    // =========================================================================

    @Test
    void jaccard_identicalDocumentsScore1() {
        FingerprintBuilder fb = new FingerprintBuilder();
        List<String> tokens = Arrays.asList("a", "b", "c", "d", "e");
        fb.addDocument(1, tokens);
        fb.addDocument(2, tokens);   // identical

        SimilarityChecker sc = new SimilarityChecker(fb);
        assertEquals(1.0, sc.jaccard(1, 2), 0.001);
    }

    @Test
    void jaccard_disjointDocumentsScore0() {
        FingerprintBuilder fb = new FingerprintBuilder();
        fb.addDocument(1, Arrays.asList("a", "b", "c", "d", "e"));
        fb.addDocument(2, Arrays.asList("x", "y", "z", "w", "v"));

        SimilarityChecker sc = new SimilarityChecker(fb);
        assertEquals(0.0, sc.jaccard(1, 2), 0.001);
    }
}
