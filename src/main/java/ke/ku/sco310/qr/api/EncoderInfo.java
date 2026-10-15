package ke.ku.sco310.qr.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * JavaBean describing the capabilities published by an encoder component.
 *
 * <p>Capability metadata is what makes <em>replaceability</em> checkable at run time:
 * an assembly can ask two candidate encoders for their {@code EncoderInfo} and decide
 * whether a swap is admissible before any payload is encoded (chapter 2: "a connection
 * exists if there is an interface offered by C1 that satisfies a dependency of C2";
 * the metadata makes the match testable).</p>
 *
 * <p>Follows the JavaBeans conventions: public class, public no-argument constructor,
 * {@code get}/{@code set} design patterns, {@link Serializable}.</p>
 */
public class EncoderInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String componentId = "";
    private String providerName = "";
    private String providerVersion = "";
    private String providerLicence = "";
    private String engineDescription = "";
    private int minimumVersion = 1;
    private int maximumVersion = 40;
    private int maximumByteCapacity = 2953;
    private boolean thirdParty = false;
    private boolean requiresOptionalLibrary = false;
    private ArrayList<QrEncodingMode> supportedModes = new ArrayList<>();

    public EncoderInfo() {
        // required by the JavaBeans specification (tools reconstitute beans through it)
    }

    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getProviderVersion() {
        return providerVersion;
    }

    public void setProviderVersion(String providerVersion) {
        this.providerVersion = providerVersion;
    }

    public String getProviderLicence() {
        return providerLicence;
    }

    public void setProviderLicence(String providerLicence) {
        this.providerLicence = providerLicence;
    }

    public String getEngineDescription() {
        return engineDescription;
    }

    public void setEngineDescription(String engineDescription) {
        this.engineDescription = engineDescription;
    }

    public int getMinimumVersion() {
        return minimumVersion;
    }

    public void setMinimumVersion(int minimumVersion) {
        this.minimumVersion = minimumVersion;
    }

    public int getMaximumVersion() {
        return maximumVersion;
    }

    public void setMaximumVersion(int maximumVersion) {
        this.maximumVersion = maximumVersion;
    }

    public int getMaximumByteCapacity() {
        return maximumByteCapacity;
    }

    public void setMaximumByteCapacity(int maximumByteCapacity) {
        this.maximumByteCapacity = maximumByteCapacity;
    }

    public boolean isThirdParty() {
        return thirdParty;
    }

    public void setThirdParty(boolean thirdParty) {
        this.thirdParty = thirdParty;
    }

    public boolean isRequiresOptionalLibrary() {
        return requiresOptionalLibrary;
    }

    public void setRequiresOptionalLibrary(boolean requiresOptionalLibrary) {
        this.requiresOptionalLibrary = requiresOptionalLibrary;
    }

    public List<QrEncodingMode> getSupportedModes() {
        return Collections.unmodifiableList(supportedModes);
    }

    public void setSupportedModes(List<QrEncodingMode> supportedModes) {
        this.supportedModes = supportedModes == null
                ? new ArrayList<QrEncodingMode>()
                : new ArrayList<>(supportedModes);
    }

    /** @param mode candidate mode @return {@code true} when this encoder can emit that mode. */
    public boolean supports(QrEncodingMode mode) {
        return supportedModes.contains(mode);
    }

    @Override
    public String toString() {
        return providerName + " " + providerVersion
                + " [versions " + minimumVersion + "-" + maximumVersion
                + ", modes " + supportedModes
                + (thirdParty ? ", third-party" : ", in-house")
                + (requiresOptionalLibrary ? ", optional library required" : "") + "]";
    }
}
