package ke.ku.sco310.qr.engine;

/**
 * Arithmetic in GF(2^8) with the QR Code primitive polynomial
 * x^8 + x^4 + x^3 + x^2 + 1 (0x11D), as required by ISO/IEC 18004 for Reed-Solomon
 * error-correction codeword generation.
 *
 * <p>Log/antilog tables are built once in a static initialiser: multiplication becomes an
 * addition of logarithms, which is both simpler to verify and faster than shifting loops.</p>
 */
final class GaloisField {

    private static final int PRIMITIVE_POLYNOMIAL = 0x11D;
    private static final int[] EXP = new int[512];
    private static final int[] LOG = new int[256];

    static {
        int x = 1;
        for (int i = 0; i < 255; i++) {
            EXP[i] = x;
            LOG[x] = i;
            x <<= 1;
            if ((x & 0x100) != 0) {
                x ^= PRIMITIVE_POLYNOMIAL;
            }
        }
        for (int i = 255; i < EXP.length; i++) {
            EXP[i] = EXP[i - 255];
        }
    }

    private GaloisField() {
    }

    /** @return {@code a * b} in GF(2^8). */
    static int multiply(int a, int b) {
        if (a == 0 || b == 0) {
            return 0;
        }
        return EXP[LOG[a] + LOG[b]];
    }

    /** @return the antilog of {@code exponent} ({@code alpha^exponent}). */
    static int exp(int exponent) {
        return EXP[exponent % 255];
    }

    /** @return the discrete logarithm of {@code value}. */
    static int log(int value) {
        if (value == 0) {
            throw new IllegalArgumentException("log(0) is undefined in GF(2^8)");
        }
        return LOG[value];
    }
}
