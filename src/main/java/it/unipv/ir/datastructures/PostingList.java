package it.unipv.ir.datastructures;

import java.util.ArrayList;
import java.util.List;

/**
 * A posting list: a sorted singly-linked list of PostingNodes,
 * ordered by ascending docId.
 *
 * This is the central data structure of the inverted index.
 * Every term in the dictionary maps to one PostingList.
 *
 * Supports:
 *   - O(1) append (when docIds arrive in sorted order during indexing)
 *   - Sequential iteration (for AND/OR merges)
 *   - Skip-pointer traversal (see SkipList.installSkips())
 *   - Position lookups (for phrase/proximity queries)
 */
public class PostingList {

    private PostingNode head;
    private PostingNode tail;
    private int         size;  // number of documents (document frequency)

    public PostingList() {
        head = null;
        tail = null;
        size = 0;
    }

    // -------------------------------------------------------------------------
    // Mutation
    // -------------------------------------------------------------------------

    /**
     * Add a posting for docId at position pos.
     * If the docId already exists at the tail, just append the position.
     * Otherwise append a new node (caller must guarantee docId >= tail.docId).
     */
    public void add(int docId, int position) {
        if (tail != null && tail.docId == docId) {
            tail.addPosition(position);
        } else {
            PostingNode node = new PostingNode(docId);
            node.addPosition(position);
            if (head == null) {
                head = node;
                tail = node;
            } else {
                tail.next = node;
                tail = node;
            }
            size++;
        }
    }

    /**
     * Add a posting with no position info (e.g. zone index, tier index).
     * If the docId already exists at the tail, increments tf only.
     */
    public void addDocId(int docId) {
        if (tail != null && tail.docId == docId) {
            tail.tf++;
        } else {
            PostingNode node = new PostingNode(docId);
            node.tf = 1;
            if (head == null) {
                head = node;
                tail = node;
            } else {
                tail.next = node;
                tail = node;
            }
            size++;
        }
    }

    // -------------------------------------------------------------------------
    // Query
    // -------------------------------------------------------------------------

    /** Find the node for docId, or null if not present. Linear scan. */
    public PostingNode find(int docId) {
        PostingNode cur = head;
        while (cur != null) {
            if (cur.docId == docId) return cur;
            if (cur.docId > docId)  return null;
            // Use skip pointer if it doesn't overshoot
            if (cur.skip != null && cur.skip.docId <= docId)
                cur = cur.skip;
            else
                cur = cur.next;
        }
        return null;
    }

    /** Returns all docIds as a list (for debugging and compression). */
    public List<Integer> toDocIdList() {
        List<Integer> ids = new ArrayList<>(size);
        PostingNode cur = head;
        while (cur != null) { ids.add(cur.docId); cur = cur.next; }
        return ids;
    }

    /** Returns all nodes as a list (used when installing skip pointers). */
    public List<PostingNode> toNodeList() {
        List<PostingNode> nodes = new ArrayList<>(size);
        PostingNode cur = head;
        while (cur != null) { nodes.add(cur); cur = cur.next; }
        return nodes;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public PostingNode getHead()  { return head; }
    public int         getSize()  { return size; }
    public boolean     isEmpty()  { return head == null; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("PostingList[");
        PostingNode cur = head;
        while (cur != null) {
            sb.append(cur.docId);
            if (cur.next != null) sb.append(", ");
            cur = cur.next;
        }
        return sb.append("]").toString();
    }
}
