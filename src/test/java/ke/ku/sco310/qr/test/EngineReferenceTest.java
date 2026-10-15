package ke.ku.sco310.qr.test;

import ke.ku.sco310.qr.api.QrCode;
import ke.ku.sco310.qr.api.QrEncodingException;
import ke.ku.sco310.qr.api.QrEncodingMode;
import ke.ku.sco310.qr.api.QrErrorCorrection;
import ke.ku.sco310.qr.components.PureJavaQrEncoderBean;
import ke.ku.sco310.qr.engine.QrTables;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Functional verification of the in-house encoder component against the independent ZXing oracle.
 *
 * <p>The checks are deliberately layered: first the ISO/IEC 18004 tables are compared with the
 * tables published by ZXing, then real symbols are decoded, then every one of the 160
 * version/level combinations is exercised at its exact byte capacity, and finally the failure
 * paths are exercised so that a "green" report cannot be produced by silent no-ops.</p>
 */
final class EngineReferenceTest {

    private EngineReferenceTest() {
    }

    static void run(TestHarness harness, List<String> tableCrossCheckCsv, List<String> capacityCsv) {
        // ---------------------------------------------------------------- table cross-check
        int mismatches = 0;
        StringBuilder detail = new StringBuilder();
        for (int version = 1; version <= 40; version++) {
            for (QrErrorCorrection level : QrErrorCorrection.values()) {
                int[] zxing = ZxingVerify.zxingCodewordCounts(version, level);
                int eccPerBlock = QrTables.eccCodewordsPerBlock(level, version);
                int blockCount = QrTables.numErrorCorrectionBlocks(level, version);
                int dataCodewords = QrTables.numDataCodewords(version, level);
                int totalCodewords = QrTables.totalCodewords(version);
                boolean same = zxing[0] == eccPerBlock && zxing[1] == blockCount
                        && zxing[2] == dataCodewords && zxing[3] == totalCodewords;
                if (!same) {
                    mismatches++;
                    detail.append(String.format("v%d-%s ", version, level.getLabel()));
                }
                tableCrossCheckCsv.add(String.format("%d,%s,%d,%d,%d,%d,%d,%d,%d,%d,%s",
                        version, level.getLabel(), zxing[0], eccPerBlock, zxing[1], blockCount,
                        zxing[2], dataCodewords, zxing[3], totalCodewords,
                        same ? "match" : "MISMATCH"));
            }
        }
        harness.check("engine", "ISO 18004 tables match ZXing for 40 versions x 4 levels",
                mismatches == 0, mismatches + " mismatching combinations " + detail);

        // ---------------------------------------------------------------- payload round trips
        String[] payloads = {
            "A",
            "HELLO WORLD",
            "1234567890",
            "123456789012345678901234567890123456789012345678901234567890",
            "https://www.ku.ac.ke/sco310/component-programming",
            "Kenyatta University - SCO310 Component Programming 2025/2026",
            "Mixed case, punctuation: .,;:!? and symbols /+-%*",
            "\u00e9\u00e8\u00ea caf\u00e9 na\u00efve r\u00e9sum\u00e9",
            "\u4f60\u597d \u4e16\u754c - Chinese text payload",
            "\ud83d\ude80 Emoji payload with a rocket",
            "tel:+254700000000",
            "WIFI:T:WPA;S:SCO310-Lab;P:component-programming;;",
            "BEGIN:VCARD\nVERSION:3.0\nFN:Component Programming\nEND:VCARD",
            "line1\nline2\tTAB\rcarriage",
            "0123456789abcdefghijklmnopqrstuvwxyz",
        };
        int decoded = 0;
        int failures = 0;
        for (String payload : payloads) {
            for (QrErrorCorrection level : QrErrorCorrection.values()) {
                try {
                    QrCode symbol = new PureJavaQrEncoderBean().encode(payload, level);
                    String back = ZxingVerify.decodeModules(symbol);
                    if (payload.equals(back)) {
                        decoded++;
                    } else {
                        failures++;
                    }
                } catch (Exception e) {
                    failures++;
                }
            }
        }
        harness.check("engine", "payloads survive the ZXing decode round trip (15 x 4)",
                failures == 0, decoded + " decoded, " + failures + " failed");

        // ---------------------------------------------------------------- mode selection
        QrCode numeric = encode("12345678901234567890", QrErrorCorrection.MEDIUM);
        harness.checkEquals("engine", "numeric payload selects numeric mode",
                QrEncodingMode.NUMERIC, numeric.getEncodingMode());
        QrCode alpha = encode("HELLO WORLD", QrErrorCorrection.MEDIUM);
        harness.checkEquals("engine", "upper-case payload selects alphanumeric mode",
                QrEncodingMode.ALPHANUMERIC, alpha.getEncodingMode());
        QrCode bytes = encode("Hello, World!", QrErrorCorrection.MEDIUM);
        harness.checkEquals("engine", "mixed case payload selects byte mode",
                QrEncodingMode.BYTE, bytes.getEncodingMode());
        harness.check("engine", "compact mode lowers the symbol version",
                numeric.getVersion() <= alpha.getVersion() + 1,
                "numeric v" + numeric.getVersion() + " vs alphanumeric v" + alpha.getVersion());

        // ---------------------------------------------------------------- capacity sweep
        int sweepOk = 0;
        int sweepFail = 0;
        for (int version = 1; version <= 40; version++) {
            for (QrErrorCorrection level : QrErrorCorrection.values()) {
                int capacityBytes = QrTables.byteCapacity(version, level);
                String payload = String.join("", Collections.nCopies(capacityBytes, "X"));
                try {
                    PureJavaQrEncoderBean encoder = new PureJavaQrEncoderBean();
                    encoder.setMinimumVersion(version);
                    encoder.setMaximumVersion(version);
                    QrCode symbol = encoder.encode(payload, level);
                    String back = ZxingVerify.decodeModules(symbol);
                    boolean ok = payload.equals(back) && symbol.getVersion() == version;
                    if (ok) {
                        sweepOk++;
                    } else {
                        sweepFail++;
                        detail.append("v").append(version).append('-').append(level.getLabel()).append(' ');
                    }
                    capacityCsv.add(String.format("%d,%s,%d,%d,%d,%d,%d,%s",
                            version, level.getLabel(), capacityBytes, symbol.getVersion(),
                            symbol.getSize(), symbol.getDataCodewordCount(), symbol.getEccCodewordCount(),
                            ok ? "decoded" : "FAILED"));
                } catch (Exception e) {
                    sweepFail++;
                    capacityCsv.add(String.format("%d,%s,%d,,,,,FAILED:%s",
                            version, level.getLabel(), capacityBytes, e.getClass().getSimpleName()));
                }
            }
        }
        harness.check("engine", "full capacity round trip for all 40 versions x 4 levels",
                sweepFail == 0, sweepOk + " decoded, " + sweepFail + " failed " + detail);

        // ---------------------------------------------------------------- determinism and masks
        QrCode first = encode("determinism check", QrErrorCorrection.QUARTILE);
        QrCode second = encode("determinism check", QrErrorCorrection.QUARTILE);
        harness.checkEquals("engine", "encoding is deterministic for equal input",
                first, second);

        int distinctMasks = 0;
        List<Integer> masks = new ArrayList<>();
        for (int mask = 0; mask < 8; mask++) {
            try {
                PureJavaQrEncoderBean encoder = new PureJavaQrEncoderBean();
                encoder.setForcedMaskPattern(mask);
                QrCode symbol = encoder.encode("mask pattern check", QrErrorCorrection.MEDIUM);
                masks.add(symbol.getMask());
                if ("mask pattern check".equals(ZxingVerify.decodeModules(symbol))) {
                    distinctMasks++;
                }
            } catch (Exception e) {
                masks.add(-1); // recorded as a failure below
            }
        }
        harness.check("engine", "every forced mask pattern still decodes (0..7)",
                distinctMasks == 8, distinctMasks + "/8 masks decoded, masks used " + masks);
        harness.checkEquals("engine", "forced mask is reported in the symbol", 5,
                encode("forced mask", QrErrorCorrection.LOW, 5).getMask());

        // ---------------------------------------------------------------- capability metadata
        PureJavaQrEncoderBean encoder = new PureJavaQrEncoderBean();
        harness.check("engine", "encoder publishes its capability metadata",
                encoder.getEncoderInfo().getMaximumVersion() == 40
                        && encoder.getEncoderInfo().supports(QrEncodingMode.BYTE)
                        && !encoder.getEncoderInfo().isThirdParty(),
                encoder.getEncoderInfo().toString());
        harness.checkEquals("engine", "maximum byte capacity reported for level L", 2953,
                QrTables.maximumByteCapacity(QrErrorCorrection.LOW));

        // ---------------------------------------------------------------- failure paths
        harness.expectFailure("engine", "empty payload is rejected",
                () -> new PureJavaQrEncoderBean().encode("", QrErrorCorrection.LOW));
        harness.expectFailure("engine", "null payload is rejected",
                () -> new PureJavaQrEncoderBean().encode(null, QrErrorCorrection.LOW));
        harness.expectFailure("engine", "over-capacity payload is rejected with a clear error",
                () -> {
                    PureJavaQrEncoderBean small = new PureJavaQrEncoderBean();
                    small.setMaximumVersion(1);
                    small.encode(String.join("", Collections.nCopies(200, "A")), QrErrorCorrection.HIGH);
                });
        harness.expectFailure("engine", "unknown error-correction label is rejected",
                () -> QrErrorCorrection.fromLabel("Z"));
        harness.expectFailure("engine", "invalid forced mask is rejected",
                () -> new PureJavaQrEncoderBean().setForcedMaskPattern(9));

        // ---------------------------------------------------------------- constrained property
        harness.expectFailure("engine", "constrained minimumVersion vetoes an inconsistent window",
                () -> {
                    PureJavaQrEncoderBean bean = new PureJavaQrEncoderBean();
                    bean.setMaximumVersion(5);
                    bean.setMinimumVersion(9);
                });
    }

    private static QrCode encode(String payload, QrErrorCorrection level) {
        try {
            return new PureJavaQrEncoderBean().encode(payload, level);
        } catch (QrEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static QrCode encode(String payload, QrErrorCorrection level, int mask) {
        try {
            PureJavaQrEncoderBean encoder = new PureJavaQrEncoderBean();
            encoder.setForcedMaskPattern(mask);
            return encoder.encode(payload, level);
        } catch (QrEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
