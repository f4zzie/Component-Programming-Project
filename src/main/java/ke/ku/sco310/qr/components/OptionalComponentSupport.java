package ke.ku.sco310.qr.components;

/**
 * Helper that answers "is the optional third-party component available in this deployment?"
 * without forcing the JVM to load the adapter class itself.
 *
 * <p>Context dependencies of a component must be explicit (chapter 2: C = (I, D, B)). The
 * assembly therefore asks this helper <em>before</em> instantiating
 * {@link ZxingQrEncoderAdapterBean}: if the library is missing the assembly simply keeps the
 * in-house encoder and records the degraded capability instead of failing to start.</p>
 */
public final class OptionalComponentSupport {

    private OptionalComponentSupport() {
    }

    /**
     * @param className fully qualified class name that must be resolvable
     * @return {@code true} when the class can be loaded by the current class loader
     */
    public static boolean isClassPresent(String className) {
        try {
            Class.forName(className, false, OptionalComponentSupport.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    /** @return {@code true} when the ZXing core library component is on the class path. */
    public static boolean isZxingAvailable() {
        return isClassPresent("com.google.zxing.qrcode.encoder.Encoder")
                && isClassPresent("com.google.zxing.qrcode.decoder.ErrorCorrectionLevel");
    }
}
