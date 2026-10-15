package ke.ku.sco310.qr.test;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.DecoderResult;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.decoder.Decoder;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.decoder.Version;

import ke.ku.sco310.qr.api.QrCode;
import ke.ku.sco310.qr.api.QrErrorCorrection;

import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

/**
 * Independent oracle used by every functional check of this suite.
 *
 * <p>All symbols produced by the project components are decoded with the third-party ZXing
 * decoder - a completely separate implementation, written by different authors from the same
 * specification. A round trip that survives this decoder proves the encoder component is
 * interoperable, not merely self-consistent.</p>
 */
final class ZxingVerify {

    private ZxingVerify() {
    }

    private static Map<DecodeHintType, Object> hints() {
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");
        return hints;
    }

    /** Decodes a module matrix directly (no image pipeline involved). */
    static String decodeModules(QrCode symbol) throws Exception {
        BitMatrix matrix = toBitMatrix(symbol);
        DecoderResult result = new Decoder().decode(matrix, hints());
        return result.getText();
    }

    /** Decodes a rendered raster image through the full image pipeline (grayscale + binarizer). */
    static String decodeImage(BufferedImage image) throws Exception {
        LuminanceSource source = new BufferedImageLuminanceSource(image);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        Result result = new QRCodeReader().decode(bitmap, hints());
        return result.getText();
    }

    /** Decodes an artifact byte array that contains a PNG image. */
    static String decodePng(byte[] pngBytes) throws Exception {
        BufferedImage image = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(pngBytes));
        if (image == null) {
            throw new IllegalStateException("The PNG artifact could not be read back");
        }
        return decodeImage(image);
    }

    static BitMatrix toBitMatrix(QrCode symbol) {
        BitMatrix matrix = new BitMatrix(symbol.getSize());
        for (int y = 0; y < symbol.getSize(); y++) {
            for (int x = 0; x < symbol.getSize(); x++) {
                if (symbol.isDark(x, y)) {
                    matrix.set(x, y);
                }
            }
        }
        return matrix;
    }

    /** @return the codeword layout ZXing expects for a version and level (table cross-check). */
    static int[] zxingCodewordCounts(int version, QrErrorCorrection level) {
        Version zxingVersion = Version.getVersionForNumber(version);
        ErrorCorrectionLevel zxingLevel = ErrorCorrectionLevel.valueOf(level.getLabel());
        Version.ECBlocks blocks = zxingVersion.getECBlocksForLevel(zxingLevel);
        int eccPerBlock = blocks.getECCodewordsPerBlock();
        int blockCount = 0;
        int dataCodewords = 0;
        for (Version.ECB ecb : blocks.getECBlocks()) {
            blockCount += ecb.getCount();
            dataCodewords += ecb.getCount() * ecb.getDataCodewords();
        }
        int total = dataCodewords + eccPerBlock * blockCount;
        return new int[]{eccPerBlock, blockCount, dataCodewords, total};
    }

    /** @return the format ZXing reports for the bitmap, used by the negative controls. */
    static String formatOf(QrCode symbol) throws Exception {
        BitMatrix matrix = toBitMatrix(symbol);
        DecoderResult result = new Decoder().decode(matrix, hints());
        return result.getECLevel();
    }

    static BarcodeFormat qrFormat() {
        return BarcodeFormat.QR_CODE;
    }
}
