package ke.ku.sco310.qr.api;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Immutable value object describing one encoded QR Code symbol.
 *
 * <p>This is the <em>data contract</em> of the suite: the encoder components produce it,
 * the renderer components consume it and the persistence component stores it. Because the
 * value object is immutable it can be shared between components without defensive copying
 * at every call site, and because it is {@link Serializable} it survives the persistence
 * boundary (JavaBeans persistence, chapter 3).</p>
 */
public final class QrCode implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String content;
    private final QrErrorCorrection requestedErrorCorrection;
    private final QrErrorCorrection errorCorrection;
    private final QrEncodingMode encodingMode;
    private final int characterCount;
    private final int version;
    private final int size;
    private final int mask;
    private final int dataCodewordCount;
    private final int eccCodewordCount;
    private final boolean[][] modules;
    private final String producerId;

    /**
     * @param content                  payload that was encoded
     * @param requestedErrorCorrection level asked for by the caller
     * @param errorCorrection          level actually written into the symbol (may be boosted)
     * @param encodingMode             segment mode chosen by the encoder
     * @param characterCount           number of characters in the segment
     * @param version                  symbol version 1..40
     * @param mask                     mask pattern 0..7 that was applied
     * @param dataCodewordCount        number of data codewords (before error correction)
     * @param eccCodewordCount         number of error-correction codewords
     * @param modules                  {@code modules[y][x] == true} means a dark module
     * @param producerId               identifier of the encoder component that built the symbol
     */
    public QrCode(String content, QrErrorCorrection requestedErrorCorrection,
                  QrErrorCorrection errorCorrection, QrEncodingMode encodingMode,
                  int characterCount, int version, int mask, int dataCodewordCount,
                  int eccCodewordCount, boolean[][] modules, String producerId) {
        this.content = content;
        this.requestedErrorCorrection = requestedErrorCorrection;
        this.errorCorrection = errorCorrection;
        this.encodingMode = encodingMode;
        this.characterCount = characterCount;
        this.version = version;
        this.size = version * 4 + 17;
        this.mask = mask;
        this.dataCodewordCount = dataCodewordCount;
        this.eccCodewordCount = eccCodewordCount;
        this.modules = new boolean[modules.length][];
        for (int y = 0; y < modules.length; y++) {
            this.modules[y] = Arrays.copyOf(modules[y], modules[y].length);
        }
        this.producerId = producerId;
    }

    public String getContent() {
        return content;
    }

    public QrErrorCorrection getRequestedErrorCorrection() {
        return requestedErrorCorrection;
    }

    public QrErrorCorrection getErrorCorrection() {
        return errorCorrection;
    }

    public QrEncodingMode getEncodingMode() {
        return encodingMode;
    }

    public int getCharacterCount() {
        return characterCount;
    }

    public int getVersion() {
        return version;
    }

    /** @return width and height of the symbol in modules ({@code 4 * version + 17}). */
    public int getSize() {
        return size;
    }

    public int getMask() {
        return mask;
    }

    public int getDataCodewordCount() {
        return dataCodewordCount;
    }

    public int getEccCodewordCount() {
        return eccCodewordCount;
    }

    public String getProducerId() {
        return producerId;
    }

    /**
     * @param x column index, 0 = leftmost module
     * @param y row index, 0 = topmost module
     * @return {@code true} when the module is dark
     */
    public boolean isDark(int x, int y) {
        if (x < 0 || y < 0 || x >= size || y >= size) {
            throw new IndexOutOfBoundsException("Module (" + x + "," + y + ") outside " + size + "x" + size);
        }
        return modules[y][x];
    }

    /** @return a defensive copy of the module matrix, indexed {@code [y][x]}. */
    public boolean[][] getModules() {
        boolean[][] copy = new boolean[modules.length][];
        for (int y = 0; y < modules.length; y++) {
            copy[y] = Arrays.copyOf(modules[y], modules[y].length);
        }
        return copy;
    }

    /** @return number of dark modules (used by the mask penalty calculation and by QA reports). */
    public int countDarkModules() {
        int count = 0;
        for (boolean[] row : modules) {
            for (boolean value : row) {
                if (value) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Renders the symbol as text art. Each module becomes two characters so that the
     * aspect ratio of the printed symbol stays square.
     *
     * @param darkGlyph  glyph(s) for a dark module
     * @param lightGlyph glyph(s) for a light module
     * @param border     number of light modules added around the symbol
     * @return a newline separated string representation
     */
    public String toAsciiArt(String darkGlyph, String lightGlyph, int border) {
        StringBuilder sb = new StringBuilder();
        for (int y = -border; y < size + border; y++) {
            for (int x = -border; x < size + border; x++) {
                boolean dark = x >= 0 && y >= 0 && x < size && y < size && modules[y][x];
                sb.append(dark ? darkGlyph : lightGlyph);
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    /** @return text art with full-block dark modules and two spaces for light modules. */
    public String toAsciiArt() {
        return toAsciiArt("\u2588\u2588", "  ", 2);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof QrCode)) {
            return false;
        }
        QrCode that = (QrCode) other;
        return version == that.version
                && mask == that.mask
                && characterCount == that.characterCount
                && encodingMode == that.encodingMode
                && errorCorrection == that.errorCorrection
                && java.util.Objects.equals(content, that.content)
                && Arrays.deepEquals(modules, that.modules);
    }

    @Override
    public int hashCode() {
        int result = java.util.Objects.hash(content, errorCorrection, encodingMode, version, mask);
        return 31 * result + Arrays.deepHashCode(modules);
    }

    @Override
    public String toString() {
        return "QrCode{version=" + version + ", size=" + size + ", ec=" + errorCorrection.getLabel()
                + ", mode=" + encodingMode + ", mask=" + mask + ", chars=" + characterCount
                + ", dataCodewords=" + dataCodewordCount + ", eccCodewords=" + eccCodewordCount
                + ", producer='" + producerId + "'}";
    }
}
