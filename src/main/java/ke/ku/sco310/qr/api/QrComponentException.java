package ke.ku.sco310.qr.api;

/**
 * Base type for every checked failure that a component in this assembly may report.
 *
 * <p>Contract reasoning (Szyperski's C = (I, D, B)): an interface is not only a set of
 * method signatures, it is also a behavioural contract. Recording the failure modes in a
 * shared exception hierarchy is what allows a component to be replaced by another one -
 * a caller written against {@link ke.ku.sco310.qr.api.QrCodeEncoder} must not have to
 * know whether a pure-Java engine or a third-party engine produced the symbol.</p>
 */
public class QrComponentException extends Exception {

    private static final long serialVersionUID = 1L;

    private final String componentId;

    public QrComponentException(String componentId, String message) {
        super(message);
        this.componentId = componentId;
    }

    public QrComponentException(String componentId, String message, Throwable cause) {
        super(message, cause);
        this.componentId = componentId;
    }

    /** @return identifier of the component instance that raised the failure. */
    public String getComponentId() {
        return componentId;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + componentId + "]: " + getMessage();
    }
}
