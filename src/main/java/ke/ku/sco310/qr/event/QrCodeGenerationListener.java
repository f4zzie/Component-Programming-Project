package ke.ku.sco310.qr.event;

import java.util.EventListener;

/**
 * Listener interface of the generation event.
 *
 * <p>Discovered by builder tools through introspection using the
 * {@code add<EventListenerType>} / {@code remove<EventListenerType>} design patterns
 * (chapter 3, "Discovering Events"): because the source exposes
 * {@code addQrCodeGenerationListener} the bean's event set is visible without a BeanInfo.</p>
 */
public interface QrCodeGenerationListener extends EventListener, java.io.Serializable {

    /**
     * Called after a symbol was encoded, rendered and (optionally) stored.
     *
     * @param event description of the completed generation
     */
    void qrCodeGenerated(QrCodeGeneratedEvent event);
}
