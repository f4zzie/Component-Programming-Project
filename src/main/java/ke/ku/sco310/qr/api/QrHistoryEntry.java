package ke.ku.sco310.qr.api;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * One record of the generation history: what was encoded, with which parameters and where
 * the artifact was stored.
 *
 * <p>Beans must persist (chapter 3, "Persistence"): this class is therefore
 * {@code Serializable}, has a no-argument constructor and exposes every field through the
 * {@code get}/{@code set} design patterns so that the history file survives serialization
 * over both the binary and the XML persistence mechanisms.</p>
 */
public class QrHistoryEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    private String content = "";
    private String errorCorrectionLabel = "M";
    private String encodingMode = "Byte";
    private int version;
    private int mask;
    private int size;
    private String artifactFileName = "";
    private String artifactFormat = "";
    private long timestamp;

    public QrHistoryEntry() {
    }

    public QrHistoryEntry(QrCode symbol, String artifactFileName) {
        this.content = symbol.getContent();
        this.errorCorrectionLabel = symbol.getErrorCorrection().getLabel();
        this.encodingMode = symbol.getEncodingMode().name();
        this.version = symbol.getVersion();
        this.mask = symbol.getMask();
        this.size = symbol.getSize();
        this.artifactFileName = artifactFileName;
        this.timestamp = System.currentTimeMillis();
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getErrorCorrectionLabel() {
        return errorCorrectionLabel;
    }

    public void setErrorCorrectionLabel(String errorCorrectionLabel) {
        this.errorCorrectionLabel = errorCorrectionLabel;
    }

    public String getEncodingMode() {
        return encodingMode;
    }

    public void setEncodingMode(String encodingMode) {
        this.encodingMode = encodingMode;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public int getMask() {
        return mask;
    }

    public void setMask(int mask) {
        this.mask = mask;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getArtifactFileName() {
        return artifactFileName;
    }

    public void setArtifactFileName(String artifactFileName) {
        this.artifactFileName = artifactFileName;
    }

    public String getArtifactFormat() {
        return artifactFormat;
    }

    public void setArtifactFormat(String artifactFormat) {
        this.artifactFormat = artifactFormat;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /** @return the timestamp formatted for reports and for the history list of the GUI assembly. */
    public String getFormattedTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(new Date(timestamp));
    }

    /** @return a short single-line summary used by the GUI history view. */
    public String getSummary() {
        String text = content.length() > 32 ? content.substring(0, 32) + "..." : content;
        return getFormattedTimestamp() + " | v" + version + "-" + errorCorrectionLabel
                + " | " + artifactFormat + " | " + text.replace('\n', ' ');
    }

    @Override
    public String toString() {
        return getSummary();
    }
}
