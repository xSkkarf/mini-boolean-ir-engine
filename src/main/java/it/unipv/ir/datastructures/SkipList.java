package it.unipv.ir.datastructures;

import java.util.List;

/**
 * Skip-list augmentation for PostingLists.
 *
 * A skip list is NOT a separate data structure here — it is a set of
 * shortcut pointers added to an existing PostingList so that AND-merge
 * can skip over large sections of the list when the target docId is far
 * ahead of the current position.
 *
 * Algorithm:
 *   For a posting list of length n, install a skip pointer at every
 *   sqrt(n)-th node. The skip pointer of node i points to node i + sqrt(n).
 *   This gives O(sqrt(n)) worst-case traversal instead of O(n).
 *
 * Usage:
 *   SkipList.installSkips(postingList);
 *   // afterwards, PostingList.find() and AND-merge use skip pointers
 */
public class SkipList {

    /**
     * Install skip pointers into a PostingList.
     * Must be called after the list is fully built (indexing complete).
     */
    public static void installSkips(PostingList list) {
        int n = list.getSize();
        if (n < 4) return;   // too small to bother

        int step = (int) Math.sqrt(n);
        List<PostingNode> nodes = list.toNodeList();

        for (int i = 0; i < nodes.size(); i++) {
            int skipTarget = i + step;
            if (skipTarget < nodes.size()) {
                nodes.get(i).skip = nodes.get(skipTarget);
            }
        }
    }

    /**
     * Merge two posting lists using skip pointers (conjunctive AND).
     * Returns a new PostingList containing only docIds present in both.
     *
     * This is the optimised intersect algorithm from Manning et al.,
     * Introduction to Information Retrieval, Chapter 1.
     */
    public static PostingList intersect(PostingList p1, PostingList p2) {
        PostingList result = new PostingList();
        PostingNode n1 = p1.getHead();
        PostingNode n2 = p2.getHead();

        while (n1 != null && n2 != null) {
            if (n1.docId == n2.docId) {
                result.addDocId(n1.docId);
                n1 = n1.next;
                n2 = n2.next;
            } else if (n1.docId < n2.docId) {
                // Try to skip ahead in p1
                if (n1.skip != null && n1.skip.docId <= n2.docId)
                    n1 = n1.skip;
                else
                    n1 = n1.next;
            } else {
                // Try to skip ahead in p2
                if (n2.skip != null && n2.skip.docId <= n1.docId)
                    n2 = n2.skip;
                else
                    n2 = n2.next;
            }
        }
        return result;
    }

    /**
     * Union of two posting lists (disjunctive OR).
     * Returns a new PostingList containing docIds present in either.
     */
    public static PostingList union(PostingList p1, PostingList p2) {
        PostingList result = new PostingList();
        PostingNode n1 = p1.getHead();
        PostingNode n2 = p2.getHead();

        while (n1 != null && n2 != null) {
            if (n1.docId == n2.docId) {
                result.addDocId(n1.docId);
                n1 = n1.next;
                n2 = n2.next;
            } else if (n1.docId < n2.docId) {
                result.addDocId(n1.docId);
                n1 = n1.next;
            } else {
                result.addDocId(n2.docId);
                n2 = n2.next;
            }
        }
        while (n1 != null) { result.addDocId(n1.docId); n1 = n1.next; }
        while (n2 != null) { result.addDocId(n2.docId); n2 = n2.next; }
        return result;
    }

    /**
     * Difference: docIds in p1 but NOT in p2.
     * Used for NOT queries.
     */
    public static PostingList difference(PostingList p1, PostingList p2) {
        PostingList result = new PostingList();
        PostingNode n1 = p1.getHead();
        PostingNode n2 = p2.getHead();

        while (n1 != null && n2 != null) {
            if (n1.docId == n2.docId) {
                n1 = n1.next;
                n2 = n2.next;
            } else if (n1.docId < n2.docId) {
                result.addDocId(n1.docId);
                n1 = n1.next;
            } else {
                n2 = n2.next;
            }
        }
        while (n1 != null) { result.addDocId(n1.docId); n1 = n1.next; }
        return result;
    }
}
