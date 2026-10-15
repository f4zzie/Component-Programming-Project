package ke.ku.sco310.qr.api;

import java.beans.PropertyChangeListener;

/**
 * Small provided interface implemented by every bean of the suite that publishes bound properties.
 *
 * <p>It exists so that a front end can subscribe to any component without knowing its concrete
 * class: {@code if (renderer instanceof BoundPropertySource)} is enough. Adding this second
 * provided interface next to {@link QrCodeRenderer} is a genuine component-model design decision -
 * a component may publish several interfaces, and a consumer may depend on the smallest one it
 * actually needs (interface segregation applied to components).</p>
 */
public interface BoundPropertySource {

    /**
     * @param listener listener that will be notified whenever a bound property changes
     */
    void addPropertyChangeListener(PropertyChangeListener listener);

    /**
     * @param listener listener to detach
     */
    void removePropertyChangeListener(PropertyChangeListener listener);
}
