package it.unipv.ir.index;

import it.unipv.ir.datastructures.BTreeDictionary;
import it.unipv.ir.datastructures.PostingList;
import it.unipv.ir.datastructures.PostingNode;
import it.unipv.ir.datastructures.SkipList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Standard inverted index.
 *
 * Maps each term to a PostingList of (docId, tf, positions).
 * The dictionary is backed by a BTreeDictionary (sorted, range-scannable).
 *
 * Also maintains:
 *   - docLengths : docId → number of tokens in document (for BM25)
 *   - docCount   : total number of indexed documents
 *
 * After all documents are indexed, call finalise() to install skip
 * pointers on all posting lists.
 */
public class InvertedIndex {

    protected final BTreeDictionary dictionary;
    protected final Map<Integer, Integer> docLengths;  // docId → token count
    protected int docCount;

    public InvertedIndex() {
        this.dictionary = new BTreeDictionary();
        this.docLengths = new HashMap<>();
        this.docCount   = 0;
    }

    // -------------------------------------------------------------------------
    // Indexing
    // -------------------------------------------------------------------------

    /**
     * Index a single document.
     *
     * @param docId   unique document identifier
     * @param tokens  preprocessed tokens in document order (with duplicates)
     */
    public void indexDocument(int docId, List<String> tokens) {
        docLengths.put(docId, tokens.size());
        docCount++;

        for (int pos = 0; pos < tokens.size(); pos++) {
            String term = tokens.get(pos);
            PostingList pl = dictionary.getOrCreate(term);
            pl.add(docId, pos + 1);   // positions are 1-indexed
        }
    }

    /**
     * Install skip pointers on all posting lists.
     * Must be called after all documents are indexed.
     */
    public void finalise() {
        BTreeDictionary.BTreeNode root = dictionary.getRoot();
        installSkipsOnSubtree(root);
    }

    // -------------------------------------------------------------------------
    // Lookup
    // -------------------------------------------------------------------------

    /** Returns the PostingList for a term, or null if absent. */
    public PostingList getPostingList(String term) {
        return dictionary.get(term);
    }

    /** Document frequency of a term (size of its posting list). */
    public int df(String term) {
        PostingList pl = dictionary.get(term);
        return pl == null ? 0 : pl.getSize();
    }

    /** Term frequency of term in docId (0 if not found). */
    public int tf(String term, int docId) {
        PostingList pl = dictionary.get(term);
        if (pl == null) return 0;
        PostingNode node = pl.find(docId);
        return node == null ? 0 : node.tf;
    }

    public int getDocCount()          { return docCount; }
    public int getDocLength(int docId){ return docLengths.getOrDefault(docId, 0); }
    public int getTermCount()         { return dictionary.size(); }
    public BTreeDictionary getDictionary() { return dictionary; }
    public Map<Integer, Integer> getDocLengths() { return docLengths; }

    /** Average document length — used by BM25. */
    public double avgDocLength() {
        if (docLengths.isEmpty()) return 0;
        long total = 0;
        for (int len : docLengths.values()) total += len;
        return (double) total / docLengths.size();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void installSkipsOnSubtree(BTreeDictionary.BTreeNode node) {
        if (node == null) return;
        for (int i = 0; i < node.keyCount; i++) {
            if (node.values[i] != null)
                SkipList.installSkips(node.values[i]);
            if (!node.isLeaf)
                installSkipsOnSubtree(node.children[i]);
        }
        if (!node.isLeaf)
            installSkipsOnSubtree(node.children[node.keyCount]);
    }
}
