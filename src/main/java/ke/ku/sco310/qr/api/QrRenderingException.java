package ke.ku.sco310.qr.api;

/** Raised when a rendered artifact cannot be produced (unsupported format, I/O failure). */
public class QrRenderingException extends QrComponentException {

    private static final long serialVersionUID = 1L;

    public QrRenderingException(String componentId, String message) {
        super(componentId, message);
    }

    public QrRenderingException(String componentId, String message, Throwable cause) {
        super(componentId, message, cause);
    }
}
