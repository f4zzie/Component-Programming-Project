package ke.ku.sco310.qr.facade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Catalogue entry of one component: identity, implementation class, the interfaces it provides,
 * the interfaces it requires and whether the deployment environment can host it right now.
 *
 * <p>Keeping this data as an explicit table (instead of discovering it lazily) is what lets the
 * assembly report {@code I} and {@code D} for every component, i.e. the two thirds of
 * Szyperski's C = (I, D, B) triple that a third-party integrator is allowed to see.</p>
 */
public class ComponentDescriptor {

    private final String role;
    private final String componentId;
    private final String implementationClass;
    private final List<String> providedInterfaces = new ArrayList<>();
    private final List<String> requiredInterfaces = new ArrayList<>();
    private final List<String> keyProperties = new ArrayList<>();
    private boolean available = true;
    private String unavailableReason = "";

    public ComponentDescriptor(String role, String componentId, String implementationClass) {
        this.role = role;
        this.componentId = componentId;
        this.implementationClass = implementationClass;
    }

    public String getRole() {
        return role;
    }

    public String getComponentId() {
        return componentId;
    }

    public String getImplementationClass() {
        return implementationClass;
    }

    public List<String> getProvidedInterfaces() {
        return Collections.unmodifiableList(providedInterfaces);
    }

    public List<String> getRequiredInterfaces() {
        return Collections.unmodifiableList(requiredInterfaces);
    }

    public List<String> getKeyProperties() {
        return Collections.unmodifiableList(keyProperties);
    }

    public boolean isAvailable() {
        return available;
    }

    public String getUnavailableReason() {
        return unavailableReason;
    }

    public ComponentDescriptor provides(String... interfaceNames) {
        Collections.addAll(providedInterfaces, interfaceNames);
        return this;
    }

    public ComponentDescriptor requires(String... interfaceNames) {
        Collections.addAll(requiredInterfaces, interfaceNames);
        return this;
    }

    public ComponentDescriptor properties(String... propertyNames) {
        Collections.addAll(keyProperties, propertyNames);
        return this;
    }

    public ComponentDescriptor unavailable(String reason) {
        this.available = false;
        this.unavailableReason = reason;
        return this;
    }

    /** @return one table row used by the assembly report and by the BeanInspector output. */
    public String toTableRow() {
        return String.format("%-11s %-30s %-14s %-46s %s",
                role, componentId, available ? "available" : "unavailable",
                String.join(", ", providedInterfaces),
                requiredInterfaces.isEmpty() ? "-" : String.join(", ", requiredInterfaces));
    }

    @Override
    public String toString() {
        return role + "=" + componentId + (available ? "" : " (unavailable: " + unavailableReason + ")");
    }
}
