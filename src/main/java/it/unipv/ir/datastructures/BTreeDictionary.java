package it.unipv.ir.datastructures;

import java.util.ArrayList;
import java.util.List;

/**
 * A dictionary mapping String terms to PostingList objects,
 * backed by a custom B-Tree.
 *
 * Why a B-Tree?
 *   - O(log_t n) lookup and insert (t = branching factor)
 *   - Keys are stored in sorted order, enabling range scans
 *     needed for wildcard queries (e.g. "comput*" → scan all
 *     keys from "comput" to "computz")
 *   - In a production system the B-Tree maps naturally to
 *     disk pages; here we keep it in memory for simplicity.
 *
 * Branching factor t = 64 (each node holds 63–127 keys).
 */
public class BTreeDictionary {

    private static final int T = 64;  // minimum degree

    private BTreeNode root;
    private int       termCount;

    public BTreeDictionary() {
        root = new BTreeNode(true);
        termCount = 0;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** Look up a term. Returns null if not found. */
    public PostingList get(String term) {
        return search(root, term);
    }

    /**
     * Return the PostingList for term, creating a new empty one
     * if the term is not yet in the dictionary.
     */
    public PostingList getOrCreate(String term) {
        PostingList pl = search(root, term);
        if (pl != null) return pl;
        pl = new PostingList();
        insert(term, pl);
        return pl;
    }

    /** Insert a term with an explicit PostingList (used during index load). */
    public void put(String term, PostingList pl) {
        PostingList existing = search(root, term);
        if (existing != null) return;   // already present
        insert(term, pl);
    }

    public boolean contains(String term) {
        return search(root, term) != null;
    }

    /**
     * Return all terms in [fromTerm, toTerm] inclusive, in sorted order.
     * Used for wildcard range scans.
     */
    public List<String> rangeKeys(String fromTerm, String toTerm) {
        List<String> result = new ArrayList<>();
        rangeSearch(root, fromTerm, toTerm, result);
        return result;
    }

    /**
     * Return all terms that start with the given prefix.
     * Implemented as a range scan [prefix, prefix + '\uffff'].
     */
    public List<String> prefixKeys(String prefix) {
        return rangeKeys(prefix, prefix + '\uffff');
    }

    public int size()      { return termCount; }
    public BTreeNode getRoot() { return root; }

    // -------------------------------------------------------------------------
    // B-Tree internals
    // -------------------------------------------------------------------------

    private PostingList search(BTreeNode node, String key) {
        int i = 0;
        while (i < node.keyCount && key.compareTo(node.keys[i]) > 0) i++;

        if (i < node.keyCount && key.equals(node.keys[i]))
            return node.values[i];

        if (node.isLeaf) return null;

        return search(node.children[i], key);
    }

    private void insert(String key, PostingList value) {
        BTreeNode r = root;
        if (r.keyCount == 2 * T - 1) {
            BTreeNode s = new BTreeNode(false);
            root = s;
            s.children[0] = r;
            splitChild(s, 0, r);
            insertNonFull(s, key, value);
        } else {
            insertNonFull(r, key, value);
        }
        termCount++;
    }

    private void insertNonFull(BTreeNode node, String key, PostingList value) {
        int i = node.keyCount - 1;
        if (node.isLeaf) {
            while (i >= 0 && key.compareTo(node.keys[i]) < 0) {
                node.keys[i + 1]   = node.keys[i];
                node.values[i + 1] = node.values[i];
                i--;
            }
            node.keys[i + 1]   = key;
            node.values[i + 1] = value;
            node.keyCount++;
        } else {
            while (i >= 0 && key.compareTo(node.keys[i]) < 0) i--;
            i++;
            if (node.children[i].keyCount == 2 * T - 1) {
                splitChild(node, i, node.children[i]);
                if (key.compareTo(node.keys[i]) > 0) i++;
            }
            insertNonFull(node.children[i], key, value);
        }
    }

    private void splitChild(BTreeNode parent, int i, BTreeNode full) {
        BTreeNode newNode = new BTreeNode(full.isLeaf);
        newNode.keyCount = T - 1;

        for (int j = 0; j < T - 1; j++) {
            newNode.keys[j]   = full.keys[j + T];
            newNode.values[j] = full.values[j + T];
        }
        if (!full.isLeaf) {
            for (int j = 0; j < T; j++)
                newNode.children[j] = full.children[j + T];
        }

        full.keyCount = T - 1;

        for (int j = parent.keyCount; j >= i + 1; j--)
            parent.children[j + 1] = parent.children[j];
        parent.children[i + 1] = newNode;

        for (int j = parent.keyCount - 1; j >= i; j--) {
            parent.keys[j + 1]   = parent.keys[j];
            parent.values[j + 1] = parent.values[j];
        }
        parent.keys[i]   = full.keys[T - 1];
        parent.values[i] = full.values[T - 1];
        parent.keyCount++;
    }

    private void rangeSearch(BTreeNode node, String from, String to,
                             List<String> result) {
        int i = 0;
        while (i < node.keyCount && node.keys[i].compareTo(from) < 0) i++;

        while (i < node.keyCount && node.keys[i].compareTo(to) <= 0) {
            if (!node.isLeaf) rangeSearch(node.children[i], from, to, result);
            result.add(node.keys[i]);
            i++;
        }
        if (!node.isLeaf) rangeSearch(node.children[i], from, to, result);
    }

    // -------------------------------------------------------------------------
    // B-Tree node
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    public static class BTreeNode {
        public final String[]      keys;
        public final PostingList[] values;
        public final BTreeNode[]   children;
        public int                 keyCount;
        public boolean             isLeaf;

        BTreeNode(boolean isLeaf) {
            this.isLeaf   = isLeaf;
            this.keyCount = 0;
            this.keys     = new String[2 * T - 1];
            this.values   = new PostingList[2 * T - 1];
            this.children = new BTreeNode[2 * T];
        }
    }
}
