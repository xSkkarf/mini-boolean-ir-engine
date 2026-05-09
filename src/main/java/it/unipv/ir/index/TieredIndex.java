package it.unipv.ir.index;

import it.unipv.ir.datastructures.BTreeDictionary;
import it.unipv.ir.datastructures.PostingList;
import it.unipv.ir.datastructures.PostingNode;
import it.unipv.ir.datastructures.SkipList;

import java.util.List;

/**
 * Tiered (impact-ordered) index.
 *
 * Documents are split into two tiers based on term frequency:
 *   Tier 1 (high-impact): postings where tf >= TF_THRESHOLD
 *   Tier 2 (low-impact):  remaining postings
 *
 * During query processing, Tier 1 is searched first. If enough results
 * are found (>= MIN_RESULTS), Tier 2 is skipped. This trades recall
 * for speed — acceptable for top-K ranking where precision matters more.
 *
 * The two tiers are stored as separate BTreeDictionary instances so
 * that Tier 1 lookups don't touch Tier 2 storage at all.
 */
public class TieredIndex {

    private static final int TF_THRESHOLD = 3;    // min tf to enter Tier 1
    private static final int MIN_RESULTS  = 10;   // skip Tier 2 if we have this many

    private final BTreeDictionary tier1 = new BTreeDictionary();
    private final BTreeDictionary tier2 = new BTreeDictionary();

    // -------------------------------------------------------------------------
    // Building — split postings at index time
    // -------------------------------------------------------------------------

    public void indexDocument(int docId, List<String> tokens) {
        // Count tf for each term in this document
        java.util.Map<String, Integer> tfMap = new java.util.HashMap<>();
        for (String t : tokens) tfMap.merge(t, 1, Integer::sum);

        for (java.util.Map.Entry<String, Integer> e : tfMap.entrySet()) {
            String term = e.getKey();
            int    tf   = e.getValue();
            if (tf >= TF_THRESHOLD) {
                tier1.getOrCreate(term).addDocId(docId);
            } else {
                tier2.getOrCreate(term).addDocId(docId);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Query — try Tier 1 first, fall through to Tier 2 if needed
    // -------------------------------------------------------------------------

    /**
     * Return posting list for term starting from Tier 1.
     * If Tier 1 has enough results, Tier 2 is not consulted.
     */
    public PostingList getPostingList(String term) {
        PostingList t1 = tier1.get(term);
        if (t1 != null && t1.getSize() >= MIN_RESULTS) return t1;

        PostingList t2 = tier2.get(term);

        if (t1 == null) return t2 != null ? t2 : new PostingList();
        if (t2 == null) return t1;
        return SkipList.union(t1, t2);
    }

    /** Get only Tier 1 (fast path for query optimisation). */
    public PostingList getTier1(String term) {
        PostingList pl = tier1.get(term);
        return pl != null ? pl : new PostingList();
    }

    /** Get only Tier 2. */
    public PostingList getTier2(String term) {
        PostingList pl = tier2.get(term);
        return pl != null ? pl : new PostingList();
    }

    public int tier1Size() { return tier1.size(); }
    public int tier2Size() { return tier2.size(); }
    public int getTfThreshold() { return TF_THRESHOLD; }
}
