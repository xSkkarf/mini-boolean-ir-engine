package it.unipv.ir.datastructures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A Trie (prefix tree) used by KGramIndex and PermutermIndex.
 *
 * Each leaf node holds a set of vocabulary terms that correspond to
 * the key string at that path. For the k-gram index the key is a
 * k-gram and the terms are all vocabulary words containing that k-gram.
 * For the permuterm index the key is a rotated form and the terms
 * are the original vocabulary words.
 *
 * Supports:
 *   - insert(key, term)        — O(|key|)
 *   - lookup(key)              — O(|key|), exact match → Set<String>
 *   - prefixLookup(prefix)     — collect all terms under a prefix
 *   - wildcardLookup(pattern)  — single '*' wildcard resolved by rotation
 */
public class Trie {

    private final TrieNode root = new TrieNode();

    /** Associate a vocabulary term with the given key string. */
    public void insert(String key, String term) {
        TrieNode cur = root;
        for (char c : key.toCharArray()) {
            cur = cur.children.computeIfAbsent(c, k -> new TrieNode());
        }
        cur.terms.add(term);
    }

    /** Exact lookup. Returns the set of terms for this key (may be empty). */
    public Set<String> lookup(String key) {
        TrieNode node = navigate(key);
        return node == null ? Set.of() : node.terms;
    }

    /**
     * Return all terms reachable from any node whose path starts with prefix.
     * Used by PermutermIndex to resolve prefix patterns like "hel$".
     */
    public Set<String> prefixLookup(String prefix) {
        TrieNode node = navigate(prefix);
        if (node == null) return Set.of();
        Set<String> result = new HashSet<>();
        collectAll(node, result);
        return result;
    }

    /** Collect all (key, terms) pairs whose key starts with prefix. */
    public List<Map.Entry<String, Set<String>>> prefixEntries(String prefix) {
        TrieNode node = navigate(prefix);
        List<Map.Entry<String, Set<String>>> result = new ArrayList<>();
        if (node != null) collectEntries(node, prefix, result);
        return result;
    }

    /** Number of distinct keys inserted. */
    public int size() {
        int[] count = {0};
        countLeaves(root, count);
        return count[0];
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private TrieNode navigate(String key) {
        TrieNode cur = root;
        for (char c : key.toCharArray()) {
            cur = cur.children.get(c);
            if (cur == null) return null;
        }
        return cur;
    }

    private void collectAll(TrieNode node, Set<String> result) {
        result.addAll(node.terms);
        for (TrieNode child : node.children.values())
            collectAll(child, result);
    }

    private void collectEntries(TrieNode node, String prefix,
            List<Map.Entry<String, Set<String>>> result) {
        if (!node.terms.isEmpty())
            result.add(Map.entry(prefix, new HashSet<>(node.terms)));
        for (Map.Entry<Character, TrieNode> e : node.children.entrySet())
            collectEntries(e.getValue(), prefix + e.getKey(), result);
    }

    private void countLeaves(TrieNode node, int[] count) {
        if (!node.terms.isEmpty()) count[0]++;
        for (TrieNode child : node.children.values())
            countLeaves(child, count);
    }

    // -------------------------------------------------------------------------
    // Node
    // -------------------------------------------------------------------------

    static class TrieNode {
        final Map<Character, TrieNode> children = new HashMap<>();
        final Set<String>              terms    = new HashSet<>();
    }
}
