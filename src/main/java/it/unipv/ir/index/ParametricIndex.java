package it.unipv.ir.index;

import it.unipv.ir.datastructures.BTreeDictionary;
import it.unipv.ir.datastructures.PostingList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parametric index for structured field queries.
 *
 * Supports exact-value queries on document metadata fields such as:
 *   category = "World War II"
 *   title contains "quantum"
 *
 * Each field has its own BTreeDictionary mapping field-value strings
 * to posting lists. This is separate from the main InvertedIndex so
 * that parametric filtering can be combined with free-text search.
 *
 * Example query:
 *   "quantum mechanics" AND category:"Physics"
 *
 * Usage:
 *   parametricIndex.indexField("category", docId, categoryTokens);
 *   PostingList pl = parametricIndex.lookup("category", "physic");
 */
public class ParametricIndex {

    // fieldName → BTreeDictionary(fieldValue → PostingList)
    private final Map<String, BTreeDictionary> fields = new HashMap<>();

    public static final String FIELD_CATEGORY = "category";
    public static final String FIELD_TITLE    = "title";

    public ParametricIndex() {
        fields.put(FIELD_CATEGORY, new BTreeDictionary());
        fields.put(FIELD_TITLE,    new BTreeDictionary());
    }

    // -------------------------------------------------------------------------
    // Building
    // -------------------------------------------------------------------------

    /**
     * Index a list of tokens for a given field and document.
     * Each token becomes a separate field-value entry.
     */
    public void indexField(String field, int docId, List<String> tokens) {
        BTreeDictionary dict = getOrCreateField(field);
        for (String token : tokens) {
            dict.getOrCreate(token).addDocId(docId);
        }
    }

    // -------------------------------------------------------------------------
    // Query
    // -------------------------------------------------------------------------

    /**
     * Return all docIds where field contains value (exact token match).
     * Returns an empty PostingList if no match.
     */
    public PostingList lookup(String field, String value) {
        BTreeDictionary dict = fields.get(field);
        if (dict == null) return new PostingList();
        PostingList pl = dict.get(value);
        return pl != null ? pl : new PostingList();
    }

    /**
     * Prefix lookup in a field — all docIds where field-value starts with prefix.
     * Uses BTreeDictionary.prefixKeys() for the range scan.
     */
    public PostingList prefixLookup(String field, String prefix) {
        BTreeDictionary dict = fields.get(field);
        if (dict == null) return new PostingList();

        PostingList result = new PostingList();
        for (String key : dict.prefixKeys(prefix)) {
            PostingList pl = dict.get(key);
            if (pl != null) {
                result = it.unipv.ir.datastructures.SkipList.union(result, pl);
            }
        }
        return result;
    }

    public void registerField(String field) {
        fields.putIfAbsent(field, new BTreeDictionary());
    }

    private BTreeDictionary getOrCreateField(String field) {
        return fields.computeIfAbsent(field, k -> new BTreeDictionary());
    }

    public Map<String, BTreeDictionary> getFields() { return fields; }
}
