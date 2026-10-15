package ke.ku.sco310.qr.api;

/** Raised when the persistence component cannot read or write an artifact or the history file. */
public class QrStorageException extends QrComponentException {

    private static final long serialVersionUID = 1L;

    public QrStorageException(String componentId, String message) {
        super(componentId, message);
    }

    public QrStorageException(String componentId, String message, Throwable cause) {
        super(componentId, message, cause);
    }
}
