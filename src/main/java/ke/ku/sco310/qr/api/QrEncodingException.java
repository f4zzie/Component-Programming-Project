package ke.ku.sco310.qr.api;

/** Raised when a payload cannot be encoded (empty data, unknown character, payload too long). */
public class QrEncodingException extends QrComponentException {

    private static final long serialVersionUID = 1L;

    public QrEncodingException(String componentId, String message) {
        super(componentId, message);
    }

    public QrEncodingException(String componentId, String message, Throwable cause) {
        super(componentId, message, cause);
    }
}
