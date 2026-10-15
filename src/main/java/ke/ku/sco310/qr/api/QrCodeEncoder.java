package ke.ku.sco310.qr.api;

/**
 * <b>Provided interface</b> of an encoder component.
 *
 * <p>Two components in this suite implement it: the in-house pure-Java engine
 * ({@code ke.ku.sco310.qr.components.PureJavaQrEncoderBean}) and an adapter that wraps the
 * third-party ZXing engine ({@code ke.ku.sco310.qr.components.ZxingQrEncoderAdapterBean}).
 * A caller holding this type cannot tell them apart apart from the metadata returned by
 * {@link #getEncoderInfo()} - which is exactly the property that component-oriented
 * programming asks for.</p>
 *
 * <p>Pre-conditions: {@code content} is non-null and non-empty; {@code level} is non-null.<br>
 * Post-conditions: the returned symbol has {@code size == 4 * version + 17}, a valid mask
 * in the range 0..7 and is fully readable by any conforming decoder.<br>
 * Error conditions: {@link QrEncodingException} when the payload is empty, cannot be
 * represented or does not fit version 40 at the requested level.</p>
 */
public interface QrCodeEncoder extends java.io.Serializable {

    /**
     * Encodes {@code content} into a QR Code symbol.
     *
     * @param content payload to encode (UTF-8 for non-ASCII input)
     * @param level   requested error-correction level
     * @return an immutable symbol description
     * @throws QrEncodingException when the payload cannot be encoded
     */
    QrCode encode(String content, QrErrorCorrection level) throws QrEncodingException;

    /** @return capability metadata of this encoder implementation. */
    EncoderInfo getEncoderInfo();

    /** @return stable identifier of the component instance, used in logs and evidence records. */
    String getComponentId();
}
