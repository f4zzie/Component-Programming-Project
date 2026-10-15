package ke.ku.sco310.qr.engine;

import ke.ku.sco310.qr.api.QrEncodingMode;
import ke.ku.sco310.qr.api.QrErrorCorrection;

/**
 * The numeric constants of ISO/IEC 18004 that parameterise the encoder for every one of the
 * 40 symbol versions.
 *
 * <p>Three facts are tabulated:</p>
 * <ol>
 *   <li>{@code ECC_CODEWORDS_PER_BLOCK} - error-correction codewords generated per block;</li>
 *   <li>{@code NUM_ERROR_CORRECTION_BLOCKS} - how many Reed-Solomon blocks the codeword
 *       sequence is split into (splitting is required from version 3 upwards so that a
 *       scratch cannot exceed the correcting capability of one block);</li>
 *   <li>the function-pattern geometry, computed arithmetically rather than tabulated
 *       ({@link #numRawDataModules(int)} and {@link #alignmentPatternPositions(int)}).</li>
 * </ol>
 *
 * <p>Row order in the first two tables is {@code L, M, Q, H} which is the declaration order
 * of {@link QrErrorCorrection}; index 0 of every row is unused padding so that a version
 * number can be used directly as the column index.</p>
 */
public final class QrTables {

    /** Error-correction codewords per block, indexed {@code [level.ordinal()][version]}. */
    static final byte[][] ECC_CODEWORDS_PER_BLOCK = {
            // 0   1   2   3   4   5   6   7   8   9  10  11  12  13  14  15  16  17  18  19  20  21  22  23  24  25  26  27  28  29  30  31  32  33  34  35  36  37  38  39  40
            {0,  7, 10, 15, 20, 26, 18, 20, 24, 30, 18, 20, 24, 26, 30, 22, 24, 28, 30, 28, 28, 28, 28, 30, 30, 26, 28, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30}, // L
            {0, 10, 16, 26, 18, 24, 16, 18, 22, 22, 26, 30, 22, 22, 24, 24, 28, 28, 26, 26, 26, 26, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28}, // M
            {0, 13, 22, 18, 26, 18, 24, 18, 22, 20, 24, 28, 26, 24, 20, 30, 24, 28, 28, 26, 30, 28, 30, 30, 30, 30, 28, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30}, // Q
            {0, 17, 28, 22, 16, 22, 28, 26, 26, 24, 28, 24, 28, 22, 24, 24, 30, 28, 28, 26, 28, 30, 24, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30}, // H
    };

    /** Number of Reed-Solomon blocks, indexed {@code [level.ordinal()][version]}. */
    static final byte[][] NUM_ERROR_CORRECTION_BLOCKS = {
            // 0  1  2  3  4  5  6  7  8  9 10  11  12  13  14  15  16  17  18  19  20  21  22  23  24  25  26  27  28  29  30  31  32  33  34  35  36  37  38  39  40
            {0, 1, 1, 1, 1, 1, 2, 2, 2, 2,  4,  4,  4,  4,  4,  6,  6,  6,  6,  7,  8,  8,  9,  9, 10, 12, 12, 12, 13, 14, 15, 16, 17, 18, 19, 19, 20, 21, 22, 24, 25}, // L
            {0, 1, 1, 1, 2, 2, 4, 4, 4, 5,  5,  5,  8,  9,  9, 10, 10, 11, 13, 14, 16, 17, 17, 18, 20, 21, 23, 25, 26, 28, 29, 31, 33, 35, 37, 38, 40, 43, 45, 47, 49}, // M
            {0, 1, 1, 2, 2, 4, 4, 6, 6, 8,  8,  8, 10, 12, 16, 12, 17, 16, 18, 21, 20, 23, 23, 25, 27, 29, 34, 34, 35, 38, 40, 43, 45, 48, 51, 53, 56, 59, 62, 65, 68}, // Q
            {0, 1, 1, 2, 4, 4, 4, 5, 6, 8,  8, 11, 11, 16, 16, 18, 16, 19, 21, 25, 25, 25, 34, 30, 32, 35, 37, 40, 42, 45, 48, 51, 54, 57, 60, 63, 66, 70, 74, 77, 81}, // H
    };

    private QrTables() {
    }

    /** @return symbol width in modules for a version: {@code 4 * version + 17}. */
    public static int sizeForVersion(int version) {
        return version * 4 + 17;
    }

    /**
     * Counts the modules that carry data or error-correction codewords by subtracting the
     * area of every function pattern (finders, separators, timing, alignment, format and
     * version information) from the total number of modules.
     *
     * @param version symbol version 1..40
     * @return number of data modules, always a multiple of 8
     */
    public static int numRawDataModules(int version) {
        if (version < 1 || version > 40) {
            throw new IllegalArgumentException("Version out of range: " + version);
        }
        int result = (16 * version + 128) * version + 64;
        if (version >= 2) {
            int numAlign = version / 7 + 2;
            result -= (25 * numAlign - 10) * numAlign - 55;
            if (version >= 7) {
                result -= 36; // two 3x6 version information blocks
            }
        }
        return result;
    }

    /** @return total codewords carried by a version (data + error correction). */
    public static int totalCodewords(int version) {
        return numRawDataModules(version) / 8;
    }

    /** @return number of error-correction codewords generated per block. */
    public static int eccCodewordsPerBlock(QrErrorCorrection level, int version) {
        return ECC_CODEWORDS_PER_BLOCK[level.ordinal()][version];
    }

    /** @return number of Reed-Solomon blocks used by a version at an error-correction level. */
    public static int numErrorCorrectionBlocks(QrErrorCorrection level, int version) {
        return NUM_ERROR_CORRECTION_BLOCKS[level.ordinal()][version];
    }

    /** @return number of data codewords available for payload at a version and level. */
    public static int numDataCodewords(int version, QrErrorCorrection level) {
        return totalCodewords(version)
                - eccCodewordsPerBlock(level, version) * numErrorCorrectionBlocks(level, version);
    }

    /**
     * @param version symbol version
     * @return centre coordinates of the alignment patterns (empty for version 1)
     */
    public static int[] alignmentPatternPositions(int version) {
        if (version < 1 || version > 40) {
            throw new IllegalArgumentException("Version out of range: " + version);
        }
        if (version == 1) {
            return new int[0];
        }
        int numAlign = version / 7 + 2;
        int step = (version == 32) ? 26 : (version * 4 + numAlign * 2 + 1) / (numAlign * 2 - 2) * 2;
        int[] result = new int[numAlign];
        result[0] = 6;
        for (int i = result.length - 1, pos = sizeForVersion(version) - 7; i >= 1; i--, pos -= step) {
            result[i] = pos;
        }
        return result;
    }

    /**
     * @param version symbol version
     * @param level   error-correction level
     * @return largest number of bytes that fit in one byte-mode segment
     */
    public static int byteCapacity(int version, QrErrorCorrection level) {
        int dataCodewords = numDataCodewords(version, level);
        // Mode indicator (4 bits) plus the byte-mode character-count field, whose width depends on
        // the version range: 8 bits for versions 1-9, 16 bits for versions 10-40.
        int overhead = 4 + QrEncodingMode.BYTE.getCharacterCountBits(version);
        return (dataCodewords * 8 - overhead) / 8;
    }

    /**
     * @param level error-correction level
     * @return largest number of bytes that fit anywhere in the symbol (version 40)
     */
    public static int maximumByteCapacity(QrErrorCorrection level) {
        return byteCapacity(40, level);
    }
}
