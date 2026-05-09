package it.unipv.ir.compression;

import java.util.ArrayList;
import java.util.List;

/**
 * Variable-byte (VByte) encoding for posting list compression.
 *
 * Instead of storing raw docIds, we store gap-encoded sequences:
 *   [3, 7, 11, 19] → gaps → [3, 4, 4, 8]
 *
 * Each integer is encoded using 1–5 bytes. The high bit of each byte
 * signals whether more bytes follow (1 = continue, 0 = last byte).
 * The lower 7 bits carry payload.
 *
 * Example: 824 = 0b0000001_1001000 → two bytes: 10000110 00111000
 *
 * Space saving: posting lists of large corpora compress 4–6× vs raw int[].
 *
 * Reference: Manning et al., Introduction to Information Retrieval, §5.3.
 */
public class VariableByteEncoder {

    // -------------------------------------------------------------------------
    // Encoding
    // -------------------------------------------------------------------------

    /**
     * Encode a list of sorted docIds as a VByte-compressed gap sequence.
     */
    public byte[] encodeDocIds(List<Integer> docIds) {
        List<Integer> gaps = toGaps(docIds);
        List<Byte> bytes = new ArrayList<>();
        for (int gap : gaps) {
            encodeInt(gap, bytes);
        }
        byte[] result = new byte[bytes.size()];
        for (int i = 0; i < bytes.size(); i++) result[i] = bytes.get(i);
        return result;
    }

    /**
     * Encode a single non-negative integer using VByte.
     */
    public byte[] encodeInt(int value) {
        List<Byte> bytes = new ArrayList<>();
        encodeInt(value, bytes);
        byte[] result = new byte[bytes.size()];
        for (int i = 0; i < bytes.size(); i++) result[i] = bytes.get(i);
        return result;
    }

    // -------------------------------------------------------------------------
    // Decoding
    // -------------------------------------------------------------------------

    /**
     * Decode a VByte-compressed byte array back to sorted docIds.
     */
    public List<Integer> decodeDocIds(byte[] data) {
        List<Integer> gaps = decodeInts(data);
        return fromGaps(gaps);
    }

    /**
     * Decode a VByte-compressed byte array to a list of raw integers.
     */
    public List<Integer> decodeInts(byte[] data) {
        List<Integer> result = new ArrayList<>();
        int value = 0;
        int shift = 0;
        for (byte b : data) {
            int unsigned = b & 0xFF;
            value |= (unsigned & 0x7F) << shift;
            shift += 7;
            if ((unsigned & 0x80) == 0) {
                // Last byte of this number
                result.add(value);
                value = 0;
                shift = 0;
            }
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Gap encoding helpers
    // -------------------------------------------------------------------------

    /** Convert sorted docIds to gap sequence. */
    public List<Integer> toGaps(List<Integer> docIds) {
        List<Integer> gaps = new ArrayList<>(docIds.size());
        int prev = 0;
        for (int id : docIds) {
            gaps.add(id - prev);
            prev = id;
        }
        return gaps;
    }

    /** Reconstruct sorted docIds from gap sequence. */
    public List<Integer> fromGaps(List<Integer> gaps) {
        List<Integer> ids = new ArrayList<>(gaps.size());
        int current = 0;
        for (int gap : gaps) {
            current += gap;
            ids.add(current);
        }
        return ids;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void encodeInt(int value, List<Byte> out) {
        while (true) {
            int bits = value & 0x7F;
            value >>>= 7;
            if (value == 0) {
                out.add((byte) bits);   // last byte: high bit = 0
                break;
            } else {
                out.add((byte) (bits | 0x80));  // more bytes: high bit = 1
            }
        }
    }
}
