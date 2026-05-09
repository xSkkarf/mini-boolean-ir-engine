package it.unipv.ir.compression;

import it.unipv.ir.datastructures.PostingList;
import it.unipv.ir.index.InvertedIndex;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Applies VByte compression to all posting lists in the inverted index.
 *
 * Stores compressed representations as byte arrays alongside the original
 * linked-list structure. In a production system you would replace the linked
 * list entirely; here we keep both so that query processing (which needs
 * the PostingNode pointers) still works after compression.
 *
 * This class demonstrates the compression logic and reports statistics:
 *   - Original size (sum of docId ints × 4 bytes)
 *   - Compressed size (sum of VByte byte arrays)
 *   - Compression ratio
 */
public class PostingCompressor {

    private final VariableByteEncoder encoder = new VariableByteEncoder();

    // term → compressed byte array of gap-encoded docIds
    private final Map<String, byte[]> compressed = new HashMap<>();

    /**
     * Compress all posting lists in the given index.
     * Stores compressed forms internally; does not modify the index.
     */
    public CompressionStats compress(InvertedIndex index) {
        compressed.clear();

        long originalBytes    = 0;
        long compressedBytes  = 0;
        int  termsCompressed  = 0;

        // Walk the B-Tree dictionary and compress every posting list
        compressSubtree(index.getDictionary().getRoot(),
                        originalBytes, compressedBytes);

        // Re-walk to collect stats
        for (Map.Entry<String, byte[]> e : compressed.entrySet()) {
            compressedBytes += e.getValue().length;
            termsCompressed++;
        }

        return new CompressionStats(originalBytes, compressedBytes,
                                    termsCompressed);
    }

    /** Retrieve compressed bytes for a term (null if not compressed). */
    public byte[] getCompressed(String term) {
        return compressed.get(term);
    }

    /** Decompress a stored compressed posting list back to sorted docIds. */
    public List<Integer> decompress(String term) {
        byte[] bytes = compressed.get(term);
        if (bytes == null) return List.of();
        return encoder.decodeDocIds(bytes);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void compressSubtree(
            it.unipv.ir.datastructures.BTreeDictionary.BTreeNode node,
            long originalBytes, long compressedBytes) {
        if (node == null) return;
        for (int i = 0; i < node.keyCount; i++) {
            String      term = node.keys[i];
            PostingList pl   = node.values[i];
            if (term != null && pl != null) {
                List<Integer> ids   = pl.toDocIdList();
                byte[]        bytes = encoder.encodeDocIds(ids);
                compressed.put(term, bytes);
            }
            if (!node.isLeaf)
                compressSubtree(node.children[i], originalBytes, compressedBytes);
        }
        if (!node.isLeaf)
            compressSubtree(node.children[node.keyCount],
                            originalBytes, compressedBytes);
    }

    // -------------------------------------------------------------------------
    // Stats value object
    // -------------------------------------------------------------------------

    public static class CompressionStats {
        public final long originalBytes;
        public final long compressedBytes;
        public final int  termsCompressed;

        CompressionStats(long orig, long comp, int terms) {
            this.originalBytes   = orig;
            this.compressedBytes = comp;
            this.termsCompressed = terms;
        }

        public double ratio() {
            if (originalBytes == 0) return 0;
            return (double) compressedBytes / originalBytes;
        }

        @Override
        public String toString() {
            return String.format(
                "PostingCompressor: %d terms | original ~%d B | "
                + "compressed %d B | ratio %.2f",
                termsCompressed, originalBytes, compressedBytes, ratio());
        }
    }
}
