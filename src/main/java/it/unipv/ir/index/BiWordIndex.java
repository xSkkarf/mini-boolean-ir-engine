package it.unipv.ir.index;

import it.unipv.ir.datastructures.BTreeDictionary;
import it.unipv.ir.datastructures.PostingList;

import java.util.List;

/**
 * BiWord (bigram phrase) index.
 *
 * For each consecutive pair of tokens (t1, t2) in a document, the
 * compound term "t1_t2" is indexed. This allows fast two-word phrase
 * queries without needing positional lookups.
 *
 * Longer phrases "t1 t2 t3" are decomposed into biwords:
 *   "t1_t2" AND "t2_t3" — both must match.
 *
 * Limitation: produces false positives for phrases longer than 2 words
 * (the biword pairs match but the middle term may not be shared).
 * For exact phrase queries, use PositionalIndex instead. BiWord is
 * kept here as described in Manning et al. §2.4 for completeness.
 */
public class BiWordIndex {

    private final BTreeDictionary dictionary = new BTreeDictionary();

    // -------------------------------------------------------------------------
    // Building
    // -------------------------------------------------------------------------

    public void indexDocument(int docId, List<String> tokens) {
        for (int i = 0; i < tokens.size() - 1; i++) {
            String biword = tokens.get(i) + "_" + tokens.get(i + 1);
            PostingList pl = dictionary.getOrCreate(biword);
            pl.addDocId(docId);
        }
    }

    // -------------------------------------------------------------------------
    // Query
    // -------------------------------------------------------------------------

    /**
     * Phrase query for an exact two-word phrase.
     * Returns the PostingList for the biword "t1_t2".
     */
    public PostingList phraseQuery(String t1, String t2) {
        PostingList pl = dictionary.get(t1 + "_" + t2);
        return pl != null ? pl : new PostingList();
    }

    /**
     * Multi-word phrase query: decompose into consecutive biwords
     * and intersect their posting lists.
     */
    public PostingList phraseQuery(List<String> terms) {
        if (terms.size() < 2) return new PostingList();

        PostingList result = dictionary.get(terms.get(0) + "_" + terms.get(1));
        if (result == null) return new PostingList();

        for (int i = 1; i < terms.size() - 1; i++) {
            String biword = terms.get(i) + "_" + terms.get(i + 1);
            PostingList pl = dictionary.get(biword);
            if (pl == null) return new PostingList();
            result = it.unipv.ir.datastructures.SkipList.intersect(result, pl);
        }
        return result;
    }

    public BTreeDictionary getDictionary() { return dictionary; }
    public int size()                      { return dictionary.size(); }
}
