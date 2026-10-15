package ke.ku.sco310.qr.store;

import java.beans.BeanDescriptor;
import java.beans.IntrospectionException;
import java.beans.IndexedPropertyDescriptor;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;

/**
 * BeanInfo that hides the indexed {@code entries} property from XML persistence.
 *
 * <p>It also keeps the <em>indexed</em> nature of the history property visible to builder tools:
 * the descriptor is an {@link IndexedPropertyDescriptor}, so introspection still reports the
 * indexed accessors and the indexed property-change events.</p>
 *
 * <p>{@code java.beans.XMLEncoder} serialises every property it can read and write. The history
 * list can contain hundreds of records, so persisting it inside the assembled bean graph would
 * bloat the XML file; the records already have their own serialized history file. Marking the
 * descriptor with {@code setValue("transient", Boolean.TRUE)} is the standard way of telling the
 * encoder to skip a property while leaving it visible in property sheets.</p>
 */
public class QrHistoryBeanBeanInfo extends SimpleBeanInfo {

    @Override
    public BeanDescriptor getBeanDescriptor() {
        BeanDescriptor descriptor = new BeanDescriptor(QrHistoryBean.class);
        descriptor.setDisplayName("QR History Bean");
        descriptor.setShortDescription("Bounded, indexed collection of generation history records.");
        return descriptor;
    }

    @Override
    public PropertyDescriptor[] getPropertyDescriptors() {
        try {
            // IndexedPropertyDescriptor (not PropertyDescriptor) keeps the indexed view of the
            // property: Introspector then reports entries[], getEntries(int), setEntries(int, ...)
            // and the fireIndexedPropertyChange support that the bean already implements.
            IndexedPropertyDescriptor entries =
                    new IndexedPropertyDescriptor("entries", QrHistoryBean.class);
            entries.setDisplayName("History entries");
            entries.setShortDescription("Indexed property; persisted in its own history file.");
            entries.setValue("transient", Boolean.TRUE);
            PropertyDescriptor maxEntries = new PropertyDescriptor("maxEntries", QrHistoryBean.class);
            maxEntries.setDisplayName("Capacity");
            maxEntries.setShortDescription("Constrained property: listeners may veto a reduction.");
            return new PropertyDescriptor[]{entries, maxEntries};
        } catch (IntrospectionException e) {
            return null;
        }
    }
}
