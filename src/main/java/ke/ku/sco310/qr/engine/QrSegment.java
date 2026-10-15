package ke.ku.sco310.qr.engine;

import ke.ku.sco310.qr.api.QrEncodingMode;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * One data segment of a QR Code symbol: a mode, a character count and the encoded bit stream.
 *
 * <p>The segment decides which of the three encoding modes is the most compact for the
 * supplied text - numeric (10 bits per 3 digits), alphanumeric (11 bits per 2 characters) or
 * byte (8 bits per byte). Choosing the smallest fitting mode is what keeps the symbol at the
 * lowest possible version, i.e. at the highest module size for a fixed print size.</p>
 */
final class QrSegment {

    /** The 45 characters of the alphanumeric mode, indexed by their 6-bit value. */
    static final String ALPHANUMERIC_CHARSET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:";

    private final QrEncodingMode mode;
    private final int characterCount;
    private final BitBuffer data;

    private QrSegment(QrEncodingMode mode, int characterCount, BitBuffer data) {
        this.mode = mode;
        this.characterCount = characterCount;
        this.data = data;
    }

    static QrSegment makeBytes(byte[] bytes) {
        BitBuffer buffer = new BitBuffer();
        buffer.appendBytes(bytes);
        return new QrSegment(QrEncodingMode.BYTE, bytes.length, buffer);
    }

    static QrSegment makeNumeric(String digits) {
        BitBuffer buffer = new BitBuffer();
        for (int i = 0; i < digits.length(); ) {
            int take = Math.min(3, digits.length() - i);
            int value = Integer.parseInt(digits.substring(i, i + take));
            buffer.appendBits(value, take == 3 ? 10 : (take == 2 ? 7 : 4));
            i += take;
        }
        return new QrSegment(QrEncodingMode.NUMERIC, digits.length(), buffer);
    }

    static QrSegment makeAlphanumeric(String text) {
        BitBuffer buffer = new BitBuffer();
        int i = 0;
        for (; i + 2 <= text.length(); i += 2) {
            int first = ALPHANUMERIC_CHARSET.indexOf(text.charAt(i));
            int second = ALPHANUMERIC_CHARSET.indexOf(text.charAt(i + 1));
            if (first < 0 || second < 0) {
                throw new IllegalArgumentException("Character outside the alphanumeric set: "
                        + text.charAt(first < 0 ? i : i + 1));
            }
            buffer.appendBits(first * 45 + second, 11);
        }
        if (i < text.length()) {
            buffer.appendBits(ALPHANUMERIC_CHARSET.indexOf(text.charAt(i)), 6);
        }
        return new QrSegment(QrEncodingMode.ALPHANUMERIC, text.length(), buffer);
    }

    /**
     * @param text payload
     * @return a single element list with the most compact segment for this payload
     */
    static List<QrSegment> makeSegments(String text) {
        List<QrSegment> result = new ArrayList<>(1);
        if (text.isEmpty()) {
            return result;
        }
        if (isNumeric(text)) {
            result.add(makeNumeric(text));
        } else if (isAlphanumeric(text)) {
            result.add(makeAlphanumeric(text));
        } else {
            result.add(makeBytes(text.getBytes(StandardCharsets.UTF_8)));
        }
        return result;
    }

    static boolean isNumeric(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return !text.isEmpty();
    }

    static boolean isAlphanumeric(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (ALPHANUMERIC_CHARSET.indexOf(text.charAt(i)) < 0) {
                return false;
            }
        }
        return !text.isEmpty();
    }

    QrEncodingMode getMode() {
        return mode;
    }

    int getCharacterCount() {
        return characterCount;
    }

    BitBuffer getData() {
        return data;
    }

    /** @param version target symbol version @return bits this segment occupies at that version. */
    int getTotalBits(int version) {
        return 4 + mode.getCharacterCountBits(version) + data.size();
    }

    @Override
    public String toString() {
        return mode + " segment (" + characterCount + " chars, " + data.size() + " bits)";
    }
}
