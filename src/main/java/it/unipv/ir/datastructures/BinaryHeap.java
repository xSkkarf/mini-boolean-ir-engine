package it.unipv.ir.datastructures;

import java.util.ArrayList;
import java.util.List;

/**
 * A generic max-heap (binary heap) backed by an ArrayList.
 *
 * Used by the ranking layer to maintain the top-K scored documents
 * without sorting the full result set. Inserting n documents and
 * extracting K takes O(n log K) time vs O(n log n) for full sort.
 *
 * To use as a min-heap (e.g. to keep K largest elements efficiently),
 * pass a comparator that reverses the natural order.
 *
 * @param <T> element type; must be Comparable
 */
public class BinaryHeap<T extends Comparable<T>> {

    private final List<T> data;
    private final boolean isMinHeap;

    /** Create a max-heap. */
    public BinaryHeap() {
        this(false);
    }

    /** Create a max-heap (isMinHeap=false) or min-heap (isMinHeap=true). */
    public BinaryHeap(boolean isMinHeap) {
        this.data      = new ArrayList<>();
        this.isMinHeap = isMinHeap;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public void insert(T item) {
        data.add(item);
        bubbleUp(data.size() - 1);
    }

    /** Remove and return the root (max for max-heap, min for min-heap). */
    public T extract() {
        if (data.isEmpty()) throw new IllegalStateException("Heap is empty");
        T root = data.get(0);
        T last = data.remove(data.size() - 1);
        if (!data.isEmpty()) {
            data.set(0, last);
            siftDown(0);
        }
        return root;
    }

    /** Peek without removing. */
    public T peek() {
        if (data.isEmpty()) throw new IllegalStateException("Heap is empty");
        return data.get(0);
    }

    public int     size()    { return data.size(); }
    public boolean isEmpty() { return data.isEmpty(); }

    /**
     * Extract all elements in heap order (largest first for max-heap).
     * Destructive — the heap is empty afterwards.
     */
    public List<T> drainSorted() {
        List<T> result = new ArrayList<>(data.size());
        while (!isEmpty()) result.add(extract());
        return result;
    }

    // -------------------------------------------------------------------------
    // Internal heap maintenance
    // -------------------------------------------------------------------------

    private void bubbleUp(int i) {
        while (i > 0) {
            int parent = (i - 1) / 2;
            if (compare(data.get(i), data.get(parent)) > 0) {
                swap(i, parent);
                i = parent;
            } else break;
        }
    }

    private void siftDown(int i) {
        int size = data.size();
        while (true) {
            int largest = i;
            int left    = 2 * i + 1;
            int right   = 2 * i + 2;
            if (left  < size && compare(data.get(left),  data.get(largest)) > 0) largest = left;
            if (right < size && compare(data.get(right), data.get(largest)) > 0) largest = right;
            if (largest == i) break;
            swap(i, largest);
            i = largest;
        }
    }

    private int compare(T a, T b) {
        int cmp = a.compareTo(b);
        return isMinHeap ? -cmp : cmp;
    }

    private void swap(int i, int j) {
        T tmp = data.get(i);
        data.set(i, data.get(j));
        data.set(j, tmp);
    }
}
