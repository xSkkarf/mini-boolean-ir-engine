package it.unipv.ir.index;

import it.unipv.ir.datastructures.BTreeDictionary;
import it.unipv.ir.datastructures.PostingList;
import it.unipv.ir.datastructures.SkipList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Zone index for weighted zone scoring.
 *
 * A "zone" is a named field of a document (title, body, categories).
 * Each zone has its own inverted index and a configurable weight.
 * The zone score for a query term in a document is:
 *
 *   score = sum over zones of (zoneWeight * tf_in_zone)
 *
 * This is used to boost results where query terms appear in the title
 * over results where they only appear in the body.
 *
 * Default zone weights (tunable):
 *   title      = 0.6
 *   body       = 0.3
 *   categories = 0.1
 */
public class ZoneIndex {

    public static final String ZONE_TITLE      = "title";
    public static final String ZONE_BODY       = "body";
    public static final String ZONE_CATEGORIES = "categories";

    private final Map<String, BTreeDictionary> zoneIndexes  = new HashMap<>();
    private final Map<String, Double>          zoneWeights  = new HashMap<>();

    public ZoneIndex() {
        // Initialise a separate dictionary per zone
        zoneIndexes.put(ZONE_TITLE,      new BTreeDictionary());
        zoneIndexes.put(ZONE_BODY,       new BTreeDictionary());
        zoneIndexes.put(ZONE_CATEGORIES, new BTreeDictionary());

        // Default weights
        zoneWeights.put(ZONE_TITLE,      0.6);
        zoneWeights.put(ZONE_BODY,       0.3);
        zoneWeights.put(ZONE_CATEGORIES, 0.1);
    }

    // -------------------------------------------------------------------------
    // Building
    // -------------------------------------------------------------------------

    public void indexZone(String zone, int docId, List<String> tokens) {
        BTreeDictionary dict = zoneIndexes.get(zone);
        if (dict == null) throw new IllegalArgumentException("Unknown zone: " + zone);
        for (String token : tokens) {
            dict.getOrCreate(token).addDocId(docId);
        }
    }

    // -------------------------------------------------------------------------
    // Scoring
    // -------------------------------------------------------------------------

    /**
     * Zone score for a single term in a single document.
     * Returns a weighted sum of zone tf values.
     */
    public double zoneScore(String term, int docId) {
        double score = 0.0;
        for (Map.Entry<String, BTreeDictionary> e : zoneIndexes.entrySet()) {
            String zone  = e.getKey();
            PostingList pl = e.getValue().get(term);
            if (pl == null) continue;
            it.unipv.ir.datastructures.PostingNode node = pl.find(docId);
            if (node == null) continue;
            score += zoneWeights.get(zone) * node.tf;
        }
        return score;
    }

    /**
     * Returns a PostingList of docIds containing term in ANY zone.
     * This is the union of posting lists across all zones.
     */
    public PostingList getPostingList(String term) {
        PostingList result = null;
        for (BTreeDictionary dict : zoneIndexes.values()) {
            PostingList pl = dict.get(term);
            if (pl == null) continue;
            result = (result == null) ? pl : SkipList.union(result, pl);
        }
        return result != null ? result : new PostingList();
    }

    /** Retrieve posting list for a specific zone only. */
    public PostingList getPostingListForZone(String zone, String term) {
        BTreeDictionary dict = zoneIndexes.get(zone);
        if (dict == null) return new PostingList();
        PostingList pl = dict.get(term);
        return pl != null ? pl : new PostingList();
    }

    public void setZoneWeight(String zone, double weight) {
        zoneWeights.put(zone, weight);
    }

    public Map<String, Double> getZoneWeights()             { return zoneWeights; }
    public Map<String, BTreeDictionary> getZoneIndexes()    { return zoneIndexes; }
}
