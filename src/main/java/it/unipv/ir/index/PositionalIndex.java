package it.unipv.ir.index;

import it.unipv.ir.datastructures.PostingList;
import it.unipv.ir.datastructures.PostingNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Positional index — extends InvertedIndex by exposing position-level
 * operations needed for phrase and proximity queries.
 *
 * The positions are already stored in every PostingNode (see PostingNode.positions).
 * This class adds the merge algorithms that operate on those position lists.
 */
public class PositionalIndex extends InvertedIndex {

    public PositionalIndex() {
        super();
    }

    // -------------------------------------------------------------------------
    // Phrase query support
    // -------------------------------------------------------------------------

    /**
     * Find all docIds where the terms appear as a consecutive phrase.
     *
     * Algorithm (Manning et al. §2.4):
     *   1. Start with posting list of terms[0].
     *   2. For each subsequent term, intersect on docId, then check that
     *      pos(term[i]) == pos(term[i-1]) + 1 for some position pair.
     *
     * @param terms preprocessed, stemmed terms of the phrase (in order)
     * @return PostingList of matching docIds
     */
    public PostingList phraseQuery(List<String> terms) {
        if (terms.isEmpty()) return new PostingList();

        PostingList result = getPostingList(terms.get(0));
        if (result == null) return new PostingList();

        for (int t = 1; t < terms.size(); t++) {
            PostingList next = getPostingList(terms.get(t));
            if (next == null) return new PostingList();
            result = intersectPositional(result, next, 1);
        }
        return result;
    }

    /**
     * Proximity query: find docIds where term1 and term2 appear within
     * k positions of each other (|pos1 - pos2| <= k).
     */
    public PostingList proximityQuery(String term1, String term2, int k) {
        PostingList p1 = getPostingList(term1);
        PostingList p2 = getPostingList(term2);
        if (p1 == null || p2 == null) return new PostingList();
        return intersectProximity(p1, p2, k);
    }

    // -------------------------------------------------------------------------
    // Positional merge algorithms
    // -------------------------------------------------------------------------

    /**
     * Intersect two posting lists requiring pos2 = pos1 + gap.
     * gap=1 means consecutive (phrase query).
     */
    private PostingList intersectPositional(PostingList p1, PostingList p2,
                                            int gap) {
        PostingList result = new PostingList();
        PostingNode n1 = p1.getHead();
        PostingNode n2 = p2.getHead();

        while (n1 != null && n2 != null) {
            if (n1.docId == n2.docId) {
                List<Integer> matches = positionsWithGap(
                    n1.positions, n2.positions, gap);
                if (!matches.isEmpty()) {
                    // Add a synthetic node: docId present, positions = matched p2 positions
                    for (int pos : matches) result.add(n1.docId, pos);
                }
                n1 = n1.next;
                n2 = n2.next;
            } else if (n1.docId < n2.docId) {
                n1 = n1.next;
            } else {
                n2 = n2.next;
            }
        }
        return result;
    }

    /**
     * Return positions from pp2 where there exists a position in pp1
     * such that pp2[j] - pp1[i] == gap.
     */
    private List<Integer> positionsWithGap(List<Integer> pp1,
                                           List<Integer> pp2,
                                           int gap) {
        List<Integer> result = new ArrayList<>();
        int i = 0, j = 0;
        while (i < pp1.size() && j < pp2.size()) {
            int diff = pp2.get(j) - pp1.get(i);
            if (diff == gap) {
                result.add(pp2.get(j));
                i++;
                j++;
            } else if (diff < gap) {
                j++;
            } else {
                i++;
            }
        }
        return result;
    }

    /**
     * Intersect two posting lists requiring |pos1 - pos2| <= k.
     */
    private PostingList intersectProximity(PostingList p1, PostingList p2,
                                           int k) {
        PostingList result = new PostingList();
        PostingNode n1 = p1.getHead();
        PostingNode n2 = p2.getHead();

        while (n1 != null && n2 != null) {
            if (n1.docId == n2.docId) {
                boolean found = proximityExists(n1.positions, n2.positions, k);
                if (found) result.addDocId(n1.docId);
                n1 = n1.next;
                n2 = n2.next;
            } else if (n1.docId < n2.docId) {
                n1 = n1.next;
            } else {
                n2 = n2.next;
            }
        }
        return result;
    }

    private boolean proximityExists(List<Integer> pp1, List<Integer> pp2,
                                    int k) {
        int i = 0, j = 0;
        while (i < pp1.size() && j < pp2.size()) {
            int diff = Math.abs(pp1.get(i) - pp2.get(j));
            if (diff <= k) return true;
            if (pp1.get(i) < pp2.get(j)) i++;
            else j++;
        }
        return false;
    }
}
