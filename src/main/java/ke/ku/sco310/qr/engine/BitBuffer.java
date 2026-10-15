package ke.ku.sco310.qr.engine;

import java.util.Arrays;

/**
 * Internal implementation detail (the "B" of the component triple C = (I, D, B)):
 * a growable, most-significant-bit-first bit stream.
 *
 * <p>This class is deliberately package private. Third-party code must not depend on it,
 * which is what allows the encoder component to change its engine without breaking any
 * consumer of the {@link ke.ku.sco310.qr.api.QrCodeEncoder} interface.</p>
 */
final class BitBuffer {

    private boolean[] bits = new boolean[64];
    private int length;

    void appendBit(boolean value) {
        ensureCapacity(length + 1);
        bits[length++] = value;
    }

    /**
     * Appends the low {@code count} bits of {@code value}, most significant bit first.
     *
     * @param value value whose bits are appended
     * @param count number of bits to append (0..32)
     */
    void appendBits(int value, int count) {
        if (count < 0 || count > 32) {
            throw new IllegalArgumentException("Bit count out of range: " + count);
        }
        ensureCapacity(length + count);
        for (int i = count - 1; i >= 0; i--) {
            bits[length++] = ((value >>> i) & 1) != 0;
        }
    }

    void appendBytes(byte[] data) {
        for (byte b : data) {
            appendBits(b & 0xFF, 8);
        }
    }

    void append(BitBuffer other) {
        ensureCapacity(length + other.length);
        for (int i = 0; i < other.length; i++) {
            bits[length++] = other.bits[i];
        }
    }

    int size() {
        return length;
    }

    boolean get(int index) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException("bit " + index + " of " + length);
        }
        return bits[index];
    }

    /** @return the stream padded with zero bits to a whole number of bytes. */
    byte[] toByteArray() {
        byte[] result = new byte[(length + 7) / 8];
        for (int i = 0; i < length; i++) {
            if (bits[i]) {
                result[i >>> 3] |= (byte) (1 << (7 - (i & 7)));
            }
        }
        return result;
    }

    private void ensureCapacity(int required) {
        if (required > bits.length) {
            bits = Arrays.copyOf(bits, Math.max(required, bits.length * 2));
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(bits[i] ? '1' : '0');
        }
        return sb.toString();
    }
}
