package it.unipv.ir.datastructures;

import java.util.ArrayList;
import java.util.List;

/**
 * A single entry in a PostingList.
 *
 * Stores:
 *   docId     — the document identifier (monotonically increasing in the list)
 *   tf        — term frequency in this document
 *   positions — list of token positions within the document (1-indexed)
 *               needed for phrase and proximity queries
 *
 * next       — pointer to the next PostingNode (linked-list linkage)
 * skip       — skip-list pointer (set by SkipList; null otherwise)
 */
public class PostingNode {

    public int          docId;
    public int          tf;
    public List<Integer> positions;

    public PostingNode  next;   // standard linked-list next pointer
    public PostingNode  skip;   // skip-list shortcut (every sqrt(n)-th node)

    public PostingNode(int docId) {
        this.docId     = docId;
        this.tf        = 0;
        this.positions = new ArrayList<>();
        this.next      = null;
        this.skip      = null;
    }

    /** Add a position occurrence and increment tf. */
    public void addPosition(int pos) {
        positions.add(pos);
        tf++;
    }

    @Override
    public String toString() {
        return "PostingNode{docId=" + docId + ", tf=" + tf + "}";
    }
}
