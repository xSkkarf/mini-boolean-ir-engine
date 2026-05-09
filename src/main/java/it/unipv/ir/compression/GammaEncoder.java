package it.unipv.ir.compression;

import java.util.ArrayList;
import java.util.List;

/**
 * Elias gamma encoding for posting list gaps.
 *
 * Gamma codes are optimal when gap values are small and follow a power-law
 * distribution (which is typical for posting lists — most terms appear
 * in few documents).
 *
 * Encoding for integer x > 0:
 *   1. Compute k = floor(log2(x))
 *   2. Write k zeros (unary length prefix)
 *   3. Write 1 bit (separator)
 *   4. Write k-bit binary representation of (x - 2^k)
 *
 * Example: x=9 → k=3 → "000" + "1" + "001" = 0001001 (7 bits)
 *
 * We pack bits into bytes, padding the last byte with zeros.
 *
 * Reference: Manning et al., §5.3.2.
 */
public class GammaEncoder {

    // -------------------------------------------------------------------------
    // Encoding
    // -------------------------------------------------------------------------

    public byte[] encodeDocIds(List<Integer> docIds) {
        List<Integer> gaps = new ArrayList<>();
        int prev = 0;
        for (int id : docIds) { gaps.add(id - prev); prev = id; }

        BitWriter writer = new BitWriter();
        for (int gap : gaps) {
            encode(gap, writer);
        }
        return writer.toByteArray();
    }

    private void encode(int x, BitWriter writer) {
        if (x <= 0) throw new IllegalArgumentException(
            "Gamma encoding requires x > 0, got: " + x);
        int k = 31 - Integer.numberOfLeadingZeros(x); // floor(log2(x))
        // Write k zeros
        for (int i = 0; i < k; i++) writer.writeBit(0);
        // Write separator 1
        writer.writeBit(1);
        // Write k-bit offset (x - 2^k)
        int offset = x - (1 << k);
        for (int i = k - 1; i >= 0; i--) {
            writer.writeBit((offset >> i) & 1);
        }
    }

    // -------------------------------------------------------------------------
    // Decoding
    // -------------------------------------------------------------------------

    public List<Integer> decodeDocIds(byte[] data, int count) {
        BitReader reader = new BitReader(data);
        List<Integer> ids = new ArrayList<>(count);
        int current = 0;
        for (int i = 0; i < count && reader.hasMore(); i++) {
            int x = decode(reader);
            current += x;
            ids.add(current);
        }
        return ids;
    }

    private int decode(BitReader reader) {
        int k = 0;
        while (reader.hasMore() && reader.readBit() == 0) k++;
        if (k == 0) return 1;
        int offset = 0;
        for (int i = 0; i < k; i++) {
            offset = (offset << 1) | reader.readBit();
        }
        return (1 << k) + offset;
    }

    // -------------------------------------------------------------------------
    // Bit-level I/O helpers
    // -------------------------------------------------------------------------

    private static class BitWriter {
        private final List<Byte> bytes = new ArrayList<>();
        private int currentByte = 0;
        private int bitPos = 7;  // next bit position (7 = MSB)

        void writeBit(int bit) {
            if (bit != 0) currentByte |= (1 << bitPos);
            bitPos--;
            if (bitPos < 0) {
                bytes.add((byte) currentByte);
                currentByte = 0;
                bitPos = 7;
            }
        }

        byte[] toByteArray() {
            List<Byte> result = new ArrayList<>(bytes);
            if (bitPos < 7) result.add((byte) currentByte); // flush partial byte
            byte[] arr = new byte[result.size()];
            for (int i = 0; i < result.size(); i++) arr[i] = result.get(i);
            return arr;
        }
    }

    private static class BitReader {
        private final byte[] data;
        private int bytePos = 0;
        private int bitPos  = 7;

        BitReader(byte[] data) { this.data = data; }

        int readBit() {
            if (!hasMore()) return 0;
            int bit = (data[bytePos] >> bitPos) & 1;
            bitPos--;
            if (bitPos < 0) { bytePos++; bitPos = 7; }
            return bit;
        }

        boolean hasMore() {
            return bytePos < data.length;
        }
    }
}
